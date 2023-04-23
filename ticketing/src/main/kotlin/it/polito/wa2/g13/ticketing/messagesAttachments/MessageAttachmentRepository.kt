package it.polito.wa2.g13.ticketing.messagesAttachments

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageAttachmentRepository : JpaRepository<MessageAttachment, String> {
}