package it.polito.wa2.g13.server.ticketing.experts

data class ExpertDTO(
    val expertId: Long?,
    val name: String,
    val surname: String,
    val sector: String,
    val email: String
)

fun Expert.toDTO(): ExpertDTO {
    return ExpertDTO(getId(), name, surname, sector, email)
}