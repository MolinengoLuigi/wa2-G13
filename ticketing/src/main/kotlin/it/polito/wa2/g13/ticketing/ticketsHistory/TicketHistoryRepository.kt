package it.polito.wa2.g13.ticketing.ticketsHistory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TicketHistoryRepository : JpaRepository<TicketHistory, String> {
}