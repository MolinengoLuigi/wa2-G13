package it.polito.wa2.g13.server.products

import it.polito.wa2.g13.server.purchase.Purchase
import jakarta.persistence.*

@Entity
@Table(name="products")
class Product (
    @Id
    @Column(updatable = false, nullable = false)
    var ean: String,
    var name: String,
    var brand: String
){
    @OneToMany(mappedBy = "product")
    val purchases: MutableSet<Purchase> = mutableSetOf()

    fun addPurchase(purch: Purchase): Unit{
        purchases.add(purch)
    }
    override fun toString(): String {
        return "ean=${ean} name=${name} brand=${brand}"
    }
}

fun ProductDTO.toProduct(): Product {
    return Product(ean, name, brand)
}