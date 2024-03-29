package it.polito.wa2.g13.server.ticketing.ticketsHistory

import it.polito.wa2.g13.server.ticketing.tickets.Ticket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TicketHistoryRepository : JpaRepository<TicketHistory, Long> {
    fun findByTicketOrderByDateTime(ticket: Ticket): List<TicketHistory>
}