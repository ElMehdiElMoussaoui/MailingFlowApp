package com.example.appmailing.TheProducts




import androidx.lifecycle.LiveData

class ProductRepository(private val productDao: ProductDao) {

    val allProducts: LiveData<List<Product>> = productDao.getAllProducts()

    suspend fun add(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun update(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun delete(product: Product) {
        productDao.deleteProduct(product)
    }
}

//object ProductRepository {
//
//    private var nextId = 6
//
//    private val _products = mutableListOf(
//        Product(1, "Premium Wireless Headphones", ProductCategory.ELECTRONICS, 299.00,
//            "Experience studio-quality sound with active noise cancellation and 30-hour battery life."),
//        Product(2, "Minimalist Smart", ProductCategory.WEARABLES, 199.00,
//            "Track your health, receive notifications, and stay connected with this elegant smart watch."),
//        Product(3, "Eco-Friendly Yoga Mat", ProductCategory.FITNESS, 45.00,
//            "Non-slip surface made from sustainable natural rubber for the ultimate practice."),
//        Product(4, "Portable Bluetooth Speaker", ProductCategory.AUDIO, 89.50,
//            "Deep bass and 360-degree sound in a rugged, waterproof design perfect for outdoor adventures."),
//        Product(5, "Ergonomic Desk Chair", ProductCategory.OFFICE, 349.00,
//            "Adjustable lumbar support and breathable mesh back for all-day comfort at your desk.")
//    )
//
//    fun getAll(): List<Product> = _products.toList()
//
//    fun getById(id: Int): Product? = _products.find { it.id == id }
//
//    fun add(product: Product): Product {
//        val p = product.copy(id = nextId++)
//        _products.add(p)
//        return p
//    }
//
//    fun update(product: Product): Boolean {
//        val idx = _products.indexOfFirst { it.id == product.id }
//        if (idx < 0) return false
//        _products[idx] = product
//        return true
//    }
//
//    fun delete(id: Int): Boolean = _products.removeIf { it.id == id }
//
//    fun filter(query: String, category: String): List<Product> {
//        return _products.filter { p ->
//            val matchCat   = category == ProductCategory.ALL || p.category == category
//            val matchQuery = query.isBlank() ||
//                p.name.contains(query, ignoreCase = true) ||
//                p.description.contains(query, ignoreCase = true) ||
//                p.category.contains(query, ignoreCase = true)
//            matchCat && matchQuery
//        }
//    }
//}
