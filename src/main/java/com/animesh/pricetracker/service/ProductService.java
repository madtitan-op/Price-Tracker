package com.animesh.pricetracker.service;

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
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository prodRepo;
    private final NotificationService notiService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, String> SELECTORS = Map.of(
            "www.amazon.in", ".a-price-whole",
            "www.flipkart.com", ".Nx9bqj.CxhGGd.yKS4la"
//            "dl.flipkart.com", ".Nx9bqj.CxhGGd.yKS4la"
    );

    public double checkPrice(int pid) {
        TrackedProduct product = prodRepo.findById(pid)
                .orElseThrow(() -> new RuntimeException("Product with ID: " + pid + " not found!!!"));
        try {
            return checkPrice(product);
        } catch (IOException e) {
            return -1;
        }
    }

    public TrackedProduct addProduct(TrackedProduct product) throws Exception{
        try {
            System.out.println("Expanding URL...");
            String expandedURL = expandURL(product.getUrl());

            if (SELECTORS.containsKey(new URL(expandedURL).getHost())) {
                product.setUrl(expandedURL);
            }
            else
                throw new IOException("This domain is currently not supported.");
        } catch (IOException e) {
            throw new RuntimeException("Could not expand URL: " + product.getUrl());
        }

        return prodRepo.save(product);
    }

    public Set<String> getSupportedDomains() {
        return SELECTORS.keySet();
    }

    private String expandURL(String shortURL) throws IOException{
        URL url = new URL(shortURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");

        connection.setInstanceFollowRedirects(false);

        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();

        System.out.println(responseCode);

        if (responseCode >= 300 && responseCode < 400) {
            String redirectedURL = connection.getHeaderField("Location");
            if (redirectedURL != null) {
                System.out.println("Redirected to: " + redirectedURL);
                return redirectedURL;
            }
        }

        return shortURL;
    }

    public void deleteProduct(int pid) {
        prodRepo.deleteById(pid);
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
                System.out.println("Checking price for: " + prod.getUrl());
                checkPrice(prod);
            } catch (IOException e) {
                System.out.println("Failed price check for: " + prod.getUrl());

                if (e.getMessage().startsWith("Could")) {
                    try {
                        long delay = 100000 + (long) (Math.random() * 15000);
                        Thread.sleep(delay);
                        i--;
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private double checkPrice(TrackedProduct trackedProduct) throws IOException{
//        TrackedProduct trackedProduct = prodRepo.findById(prodId)
//                .orElseThrow(() -> new RuntimeException("Product Doesn't Exist!!!"));

//        try {

            URL url = new URL(trackedProduct.getUrl());
            String domain = url.getHost();
            String selector = SELECTORS.get(domain);

            if (selector == null) {
                throw new IOException("Domain not supported: " + domain);
            }

            Document doc = Jsoup.connect(trackedProduct.getUrl()).get();
            Element priceElement = doc.selectFirst(selector);

            if (priceElement == null) {
                throw new IOException("Could not find price element for " + url);
            }

            String rawPriceText = priceElement.text();
            String priceText = rawPriceText.replaceAll("[^\\d.]", "");
            double curPrice = Double.parseDouble(priceText);

            System.out.println("Current Price: " + curPrice + " : " + trackedProduct.getUrl());

            if (curPrice <= trackedProduct.getTargetPrice()) {
                notiService.sendPriceAlert(trackedProduct);
            }
            return curPrice;

//        } catch (IOException e) {
//            System.err.println("Error fetching URL: " + trackedProduct.getUrl());
//            e.printStackTrace();
//        } catch (Exception e) {
//            System.err.println("Error parsing product: " + trackedProduct.getUrl());
//            e.printStackTrace();
//        }
//
//        return -1;
    }
}
