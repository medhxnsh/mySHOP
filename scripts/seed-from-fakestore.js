const axios = require('axios');

async function seed() {
    const adminToken = process.env.ADMIN_TOKEN;
    if (!adminToken) {
        console.error('Error: ADMIN_TOKEN environment variable is required.');
        console.error('Usage: ADMIN_TOKEN=<jwt> node seed-from-fakestore.js');
        process.exit(1);
    }

    const api = axios.create({
        baseURL: 'http://localhost:8080/api/v1',
        headers: { Authorization: `Bearer ${adminToken}` }
    });

    // Get our categories first
    const { data: catRes } = await api.get('/categories');
    const categories = catRes.data;

    // Map FakeStore categories to our slugs
    const categoryMap = {
        "electronics": categories.find(c => c.slug === 'electronics')?.id,
        "jewelery": categories.find(c => c.slug === 'accessories')?.id,
        "men's clothing": categories.find(c => c.slug === 'clothing')?.id,
        "women's clothing": categories.find(c => c.slug === 'clothing')?.id,
    };

    // If 'accessories' category doesn't exist, create it
    if (!categoryMap["jewelery"]) {
        const { data: newCat } = await api.post('/categories', {
            name: 'Accessories',
            slug: 'accessories'
        });
        categoryMap["jewelery"] = newCat.data.id;
    }

    // Fetch FakeStoreAPI products
    const { data: fakeProducts } = await axios.get('https://fakestoreapi.com/products');

    for (const p of fakeProducts) {
        try {
            await api.post('/products', {
                name: p.title,
                description: p.description,
                price: p.price,
                stockQuantity: Math.floor(Math.random() * 100) + 10,
                sku: `FAKESTORE-${p.id}`,
                imageUrl: p.image,
                categoryId: categoryMap[p.category]
            });
            console.log(`Created: ${p.title}`);
        } catch (e) {
            console.error(`Failed: ${p.title}`, e.response?.data?.error?.message);
        }
    }

    console.log('\nSeeding complete.');
}

seed().catch(console.error);
