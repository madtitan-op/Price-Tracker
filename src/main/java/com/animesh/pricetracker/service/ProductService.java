package com.animesh.pricetracker.service;

import com.animesh.pricetracker.dto.ProdRequestDTO;
import com.animesh.pricetracker.dto.ProdResponseDTO;
import com.animesh.pricetracker.exception.ProductAlreadyExists;
import com.animesh.pricetracker.exception.ScrapingFailedException;
import com.animesh.pricetracker.model.TrackedProduct;
import com.animesh.pricetracker.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository prodRepo;
    private final NotificationService notiService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, String> PRICE_SELECTORS = Map.of(
            "www.amazon.in", ".a-price-whole",
            "www.flipkart.com", ".Nx9bqj.CxhGGd.yKS4la"
    );

    public ProdResponseDTO addProduct(ProdRequestDTO requestDTO) throws IOException {

        TrackedProduct tp = toProduct(requestDTO);

        if (PRICE_SELECTORS.containsKey(tp.getSite())) {
            if (prodRepo.existsTrackedProductBySid(tp.getSid())) {
                throw new ProductAlreadyExists("Product already added");
            }
            return toResponseDTO(prodRepo.save(tp));
        }
        else {
            throw new IOException("Something went wrong");
        }
    }

    public List<ProdResponseDTO> getAllProducts() {
        List<TrackedProduct> allProds = prodRepo.findAll();
        return allProds.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public double checkPrice(int pid) {
        TrackedProduct product = prodRepo.findById(pid)
                .orElseThrow(() -> new RuntimeException("Product with ID: " + pid + " not found!!!"));
        try {
            return checkPrice(product);
        } catch (IOException e) {
            return -1;
        }
    }

    public void deleteProduct(int pid) {
        prodRepo.deleteById(pid);
    }

    public Set<String> getSupportedDomains() {
        return PRICE_SELECTORS.keySet();
    }

/*    public TrackedProduct addProduct(TrackedProduct product) throws Exception {
        try {
            System.out.println("Expanding URL...");
            String expandedURL = expandURL(product.getUrl());

            String cleanURL = cleanURL(expandedURL);

            if (PRICE_SELECTORS.containsKey(new URL(expandedURL).getHost())) {
                product.setUrl(expandedURL);
            } else
                throw new IOException("This domain is currently not supported.");
        } catch (IOException e) {
            throw new RuntimeException("Could not expand URL: " + product.getUrl());
        }

        return prodRepo.save(product);
    }*/

    private String expandURL(String shortURL) throws IOException {
        URL url = new URL(shortURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");

        connection.setInstanceFollowRedirects(false);

        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode >= 300 && responseCode < 400) {
            String redirectedURL = connection.getHeaderField("Location");
            if (redirectedURL != null) {
                System.out.println("Redirected to: " + redirectedURL);
                return redirectedURL;
            }
        }

        return shortURL;
    }

    @Scheduled(fixedRate = 1000 * 60 * 60)     //time interval is in ms - current: 1hr
    public void checkAllProducts() {
        System.out.println("Running Scheduled Price Check...");
        List<TrackedProduct> products = prodRepo.findAll();

        Map<String, List<TrackedProduct>> prodByDomain = products.stream()
                .collect(Collectors.groupingBy(p -> {
                    try {
                        return new URL(p.getUrl()).getHost();
                    } catch (Exception e) {
                        return "invalid-domain";
                    }
                }));

        for (Map.Entry<String, List<TrackedProduct>> entry : prodByDomain.entrySet()) {
            String domain = entry.getKey();
            List<TrackedProduct> domainProds = entry.getValue();

            if (domain.equals("invalid-domain")) continue;

            executorService.submit(() -> {
                System.out.println("Starting checks for domain: " + domain);
                processDomainProds(domainProds);
            });
        }

        /*for (TrackedProduct product : products) {
            checkPrice(product);
        }*/
    }

    private void processDomainProds(List<TrackedProduct> prods) {
        for (int i = 0; i < prods.size(); i++) {
            TrackedProduct prod = prods.get(i);

            try {
                System.out.println("Checking price for: " + prod.getSid());
                checkPrice(prod);
            } catch (ScrapingFailedException e) {
                try {
                    long delay = 100000 + (long) (Math.random() * 15000);
                    Thread.sleep(delay);
                    i--;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                System.out.println("Failed price check for: " + prod.getUrl());
            }
        }
    }

    private double checkPrice(TrackedProduct trackedProduct) throws IOException {

        URL url = new URL(trackedProduct.getUrl());
        String domain = url.getHost();
        String selector = PRICE_SELECTORS.get(domain);

        if (selector == null) {
            throw new IOException("Domain not supported: " + domain);
        }

        Document doc = Jsoup.connect(trackedProduct.getUrl()).get();
        Element priceElement = doc.selectFirst(selector);

        if (priceElement == null) {
            throw new ScrapingFailedException("Could not find price element for " + url);
        }

        String rawPriceText = priceElement.text();
        String priceText = rawPriceText.replaceAll("[^\\d.]", "");
        double curPrice = Double.parseDouble(priceText);

        System.out.println("Current Price: " + curPrice + " : " + trackedProduct.getSid());

        if (curPrice <= trackedProduct.getTargetPrice()) {
            notiService.sendPriceAlert(trackedProduct);
        }
        return curPrice;
    }

    private TrackedProduct toProduct(ProdRequestDTO dto) throws IOException {
        String strURL = expandURL(dto.url());
        URL url = new URL(strURL);
        String cleanUrl = url.getProtocol() + "://" + url.getHost() + url.getPath();

        return new TrackedProduct(
                extractProductId(cleanUrl),
                url.getHost(),
                cleanUrl,
                dto.userEmail(),
                dto.targetPrice()
        );
    }

    private ProdResponseDTO toResponseDTO(TrackedProduct product) {
        return new ProdResponseDTO(
                product.getId(),
                product.getSid(),
                product.getSite(),
                product.getUrl(),
                product.getTargetPrice()
        );
    }

    private String extractProductId(String urlString) throws MalformedURLException {

        String exp;
        URL url = new URL(urlString);

        if (url.getHost().equals("www.amazon.in")) {
            exp = "dp/([A-Z0-9]{10})";
        }
        else exp = "/p/itm([a-z0-9]{13})";

        Pattern pattern = Pattern.compile(exp);
        Matcher matcher = pattern.matcher(urlString);

        if (matcher.find()) {
            return matcher.group(1);
        }

        int lastSlashIndex = urlString.lastIndexOf('/');

        if (lastSlashIndex != -1 && lastSlashIndex < urlString.length() - 1) {
            return urlString.substring(lastSlashIndex + 1);
        }

        return null;
    }

 /*   private String cleanURL(String strURL) throws MalformedURLException {
        URL url = new URL(strURL);
        return url.getProtocol() + "://" + url.getHost() + url.getPath();
    }*/
}
