package it.polito.wa2.g13.server.ticketing.attachments

import org.springframework.stereotype.Service

@Service
class AttachmentServiceImpl(
    private val attachmentRepository: AttachmentRepository
) : AttachmentService