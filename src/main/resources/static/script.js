// --- CONFIGURATION ---
const API_BASE_URL = 'http://localhost:8080/api/products';

// --- DOM ELEMENTS ---
const addProductForm = document.getElementById('addProductForm');
const productList = document.getElementById('productList');
const loader = document.getElementById('loader');
const messageArea = document.getElementById('messageArea');
const submitButton = document.getElementById('submitButton');
const submitButtonText = document.getElementById('submitButtonText');
const submitSpinner = document.getElementById('submitSpinner');

// --- FUNCTIONS ---

/**
 * Shows a message to the user (e.g., for success or error).
 * @param {string} message - The message to display.
 * @param {boolean} isError - True if the message is an error, false otherwise.
 */
function showMessage(message, isError = false) {
    messageArea.textContent = message;
    messageArea.className = `p-4 mb-6 rounded-md text-sm ${isError ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'}`;
    messageArea.classList.remove('hidden');
// Hide the message after 5 seconds
    setTimeout(() => {
        messageArea.classList.add('hidden');
    }, 5000);
}

/**
 * Toggles the loading state of the submit button.
 * @param {boolean} isLoading - True to show spinner, false to show text.
 */
function setSubmitLoading(isLoading) {
    if (isLoading) {
        submitButton.disabled = true;
        submitButtonText.classList.add('hidden');
        submitSpinner.classList.remove('hidden');
    } else {
        submitButton.disabled = false;
        submitButtonText.classList.remove('hidden');
        submitSpinner.classList.add('hidden');
    }
}

/**
 * Fetches all tracked products from the API and displays them.
 */
async function fetchAndDisplayProducts() {
    loader.style.display = 'flex';
    productList.innerHTML = ''; // Clear existing list
    try {
        const response = await fetch(API_BASE_URL + "/all");
        if (!response.ok) {
            throw new Error('Failed to fetch products.');
        }
        const products = await response.json();

        if (products.length === 0) {
            productList.innerHTML = '<p class="text-gray-500 col-span-full text-center">No products are being tracked yet. Add one above!</p>';
        } else {
            products.forEach(product => {
                const productCard = `
                        <div class="material-card p-5 flex flex-col justify-between" id="product-${product.id}">
                            <div>
<!--                                <p class="text-sm text-gray-500 break-words">ID: ${product.id}</p>-->
                                <a href="${product.url}" target="_blank" class="text-blue-600 hover:underline font-medium break-words">${product.url}</a>
                                <p class="text-lg font-semibold my-2">Target Price: ₹${product.targetPrice.toFixed(2)}</p>
<!--                                <p class="text-sm text-gray-600">Email: ${product.userEmail}</p>-->
                            </div>
                            <div class="mt-4 flex justify-end space-x-2">
                                <button onclick="deleteProduct(${product.id})" class="text-sm py-2 px-4 material-button delete-button">Delete</button>
                            </div>
                        </div>
                    `;
                productList.innerHTML += productCard;
            });
        }
    } catch (error) {
        showMessage('Error: Could not load tracked products. Is the backend running?', true);
        productList.innerHTML = '<p class="text-red-500 col-span-full text-center">Failed to load products.</p>';
    } finally {
        loader.style.display = 'none';
    }
}

/**
 * Handles the form submission to add a new product.
 * @param {Event} event - The form submission event.
 */
async function handleAddProduct(event) {
    event.preventDefault();
    setSubmitLoading(true);

    const formData = new FormData(addProductForm);
    const productData = {
        url: formData.get('url'),
        targetPrice: parseFloat(formData.get('targetPrice')),
        userEmail: formData.get('userEmail')
    };

    try {
        const response = await fetch(`${API_BASE_URL}/add`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(productData),
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to add product.');
        }

        showMessage('Product added successfully!', false);
        addProductForm.reset();
        fetchAndDisplayProducts(); // Refresh the list
    } catch (error) {
        showMessage(`Error: ${error.message}`, true);
    } finally {
        setSubmitLoading(false);
    }
}

/**
 * Deletes a product by its ID.
 * @param {number} productId - The ID of the product to delete.
 */
async function deleteProduct(productId) {
    if (!confirm('Are you sure you want to stop tracking this product?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/delete/${productId}`, {
            method: 'DELETE',
        });

        if (!response.ok) {
            throw new Error('Failed to delete the product.');
        }

        showMessage('Product deleted successfully.', false);
// Remove the card from the UI directly for a faster response
        const cardToRemove = document.getElementById(`product-${productId}`);
        if (cardToRemove) {
            cardToRemove.remove();
        }
    } catch (error) {
        showMessage(`Error: ${error.message}`, true);
    }
}

// --- INITIALIZATION ---

// Add event listener for the form
addProductForm.addEventListener('submit', handleAddProduct);

// Fetch products when the page loads
document.addEventListener('DOMContentLoaded', fetchAndDisplayProducts);
