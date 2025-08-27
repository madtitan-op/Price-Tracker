document.addEventListener('DOMContentLoaded', () => {

    // --- App State & Configuration ---
    const app = {
        apiUrl: 'http://localhost:8080/api/products',
        elements: {
            form: document.getElementById('addProductForm'),
            productList: document.getElementById('productList'),
            loader: document.getElementById('loader'),
            toastContainer: document.getElementById('toastContainer'),
            submitButton: document.getElementById('submitButton'),
            submitButtonText: document.querySelector('#submitButton .btn-text'),
            submitSpinner: document.querySelector('#submitButton .spinner'),
        }
    };

    // --- Core Functions ---

    /**
     * Shows a toast notification.
     * @param {string} message The message to display.
     * @param {string} type 'success' or 'error'.
     */
    function showToast(message, type = 'success') {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.textContent = message;
        app.elements.toastContainer.appendChild(toast);
        setTimeout(() => toast.remove(), 5000);
    }

    /**
     * Toggles the loading state of the submit button.
     * @param {boolean} isLoading True to show spinner, false to show text.
     */
    function setSubmitLoading(isLoading) {
        app.elements.submitButton.disabled = isLoading;
        app.elements.submitButtonText.classList.toggle('hidden', isLoading);
        app.elements.submitSpinner.classList.toggle('hidden', !isLoading);
    }

    /**
     * Creates the HTML for a single product card.
     * @param {object} product The product data.
     * @returns {string} The HTML string for the card.
     */
    function createProductCard(product) {
        // Fallback for older product structures before the 'sid' field
        const productId = product.id || product.sid;
        return `
            <div class="product-card" id="product-${productId}">
                <div>
                    <a href="${product.url}" target="_blank" class="url">${product.url}</a>
                    <p class="info">Site: ${product.site || 'Unknown Site'}</p>
                    <p class="price">Target: ₹${product.targetPrice.toFixed(2)}</p>
                </div>
                <div class="actions">
                    <button class="btn btn-danger" onclick="app.deleteProduct('${productId}')">
                        <span class="material-icons">delete</span> Delete
                    </button>
                </div>
            </div>
        `;
    }

    // --- API Interaction Functions ---

    app.fetchProducts = async function() {
        app.elements.loader.style.display = 'flex';
        app.elements.productList.innerHTML = '';
        try {
            const response = await fetch(`${app.apiUrl}/all`);
            if (!response.ok) throw new Error('Failed to fetch products from the server.');
            const products = await response.json();

            if (products.length === 0) {
                app.elements.productList.innerHTML = '<p style="grid-column: 1 / -1; text-align: center;">No products are being tracked yet.</p>';
            } else {
                app.elements.productList.innerHTML = products.map(createProductCard).join('');
            }
        } catch (error) {
            showToast('Error: Could not load products. Is the backend running?', 'error');
        } finally {
            app.elements.loader.style.display = 'none';
        }
    };

    app.addProduct = async function(event) {
        event.preventDefault();
        setSubmitLoading(true);

        const formData = new FormData(app.elements.form);
        const productData = {
            url: formData.get('url'),
            targetPrice: parseFloat(formData.get('targetPrice')),
            userEmail: formData.get('userEmail')
        };

        try {
            const response = await fetch(`${app.apiUrl}/add`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(productData),
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || 'Failed to add product.');
            }

            showToast('Product added successfully!', 'success');
            app.elements.form.reset();
            this.fetchProducts(); // Refresh the list
        } catch (error) {
            showToast(`Error: ${error.message}`, 'error');
        } finally {
            setSubmitLoading(false);
        }
    };

    app.deleteProduct = async function(productId) {
        if (!confirm('Are you sure you want to stop tracking this product?')) return;

        try {
            const response = await fetch(`${app.apiUrl}/delete/${productId}`, {
                method: 'DELETE',
            });

            if (!response.ok) throw new Error('Failed to delete the product.');

            showToast('Product tracking stopped.', 'success');
            const cardToRemove = document.getElementById(`product-${productId}`);
            if (cardToRemove) cardToRemove.remove();

        } catch (error) {
            showToast(`Error: ${error.message}`, 'error');
        }
    };

    // --- Initialization ---
    function init() {
        // Expose deleteProduct to the global scope so inline onclick can find it
        window.app = { deleteProduct: app.deleteProduct };

        app.elements.form.addEventListener('submit', app.addProduct.bind(app));
        app.fetchProducts();
    }

    init();
});
