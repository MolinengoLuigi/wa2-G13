package it.polito.wa2.g13.server.ticketing.tickets

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TicketPostDTO(
    @field:NotBlank(message = "The inserted input is not valid!")
    var email: String,
    @field:Size(min=15, max=15, message = "The inserted input is not valid!")
    var ean: String
)