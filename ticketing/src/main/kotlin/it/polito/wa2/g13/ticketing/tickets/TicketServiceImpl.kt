package it.polito.wa2.g13.ticketing.tickets

import org.springframework.stereotype.Service

@Service
class TicketServiceImpl(
    private val ticketRepository: TicketRepository
) : TicketService {

}