# Web Applications II - Group 13 - Lab02

Group members:
 - 301247, MOHAMED AMINE HAMDI
 - 303474, LUIGI GIUSEPPE DIEGO MOLINENGO
 - 304684, GIOVANNI GENNA
 - 306151, MASSIMILIANO PELLEGRINO

GitHub Repository: https://github.com/MolinengoLuigi/wa2-G13


### TICKETING APIs LIST
- POST `/API/tickets/`
  - Request parameters: none
  - Request body `TicketPostDto`
  - Result:
    - status: `201` if tickets is correctly created
      - body: `TicketDTO`
    - status: `422` if body validation fails
    - status: `404` if product or profile contained in `TicketPostDto` don't exist
- PUT `/API/tickets/{ticketId}/changeStatus`
  - Request parameters: id of the target ticket
  - Request body: `{"status": "in_progress"} (example)`
  - Result:
    - status: `200` if modify succeeds
      - body: `true`
    - status: `422` if body validation fails
    - status: `404` if ticket in request param doesn't exist
    - status: `409` if ticket state change is not allowed
- PUT `/API/tickets/{ticketId}/changePriority`
  - Request parameters: id of the target ticket
  - Request body: `{"priorityLevel": 0} (example)`
  - Result:
    - status: `200` if modify succeeds
      - body: `true`
    - status: `422` if body validation fails
    - status: `404` if ticket in request param doesn't exist
- PUT `/API/tickets/{ticketId}/changeExpert`
  - Request parameters: id of the target ticket
  - Request body: `{"expertId": 1} (example)`
  - Result:
    - status: `200` if modify succeeds
      - body: `true`
    - status: `422` if body validation fails
    - status: `404` if ticket in request param or expert in body don't exist
- GET `/API/tickets/`
  - Request parameters: none
  - Request body: none
  - Result:
    - status: `200`
      - body: `List<TicketDTO>`
- GET `/API/tickets/{ticketId}`
  - Request parameters: ID of the desired ticket
  - Request body: none
  - Result:
    - status: `200`
      - body: `TicketDTO`
    - status: `404` if ticket not found
- GET `/API/tickets/API/tickets/?ean={ean}&profileId={profileId}&priorityLevel={priorityLevel}&expertId={expertId}&status={status}&creationDateStart={creationDateStart}&creationDateStop={creationDateStop}`
  - Request parameters: ean, profileId, priorityLevel, expertId, status, creationDateStart & creationDateStop to use for searching the tickets
  - Request body: none
  - Result:
    - status: `200`
        - body: `List<TicketDTO>`
    - status: `422` if priorityLevel outside `[0-4]` or if status not in `[open-closed-resolved-in_progress-reopened]` or if profileId not in email format
- PUT `/API/ticket/`
  - Request parameters: none
  - Request body: `TicketDTO` ticket to modify
  - Result:
    - status: `200`
        - body: `true` if ticket exist and modify succeeds
    - status: `422` if ticket validation fails
    - status: `404` if no ticket are found with the passed ticketId
- GET `/API/experts`
- Request parameters: none
- Request body: none
- Result:
  - status: `200`
    - body: `List<ExpertDTO>`
- POST `/API/experts/`
  - Request parameters: none
  - Request body: `ExpertDTO` expert to create
  - Result:
    - status: `201`
      - body: `true` if expert creation succeeds
    - status: `422` if expert validation fails
    - status: `409` if an expert with the selected email address already exists
- GET `/API/experts/{id}`
  - Request parameters: ID of the desired expert
  - Request body: none
  - Result:
    - status: `200`
      - body: `ExpertDTO`
    - status: `404` if no expert is found with the passed expertId
- PUT `/API/experts/{id}`
  - Request parameters: ID of the desired expert
  - Request body: ExpertDTO
  - Result:
    - status: `200`
      - body: true
    - status: `404` if no experts are found with the passed expertId
    - status: `409` if an expert with the selected email address already exists
    - status: `422` if expert validation fails
- GET `/API/experts/?sectorName={sectorName}`
  - Request parameters: sector name
  - Request body: none
  - Result:
    - status: `200`
      - body: `List<ExpertDTO>`
    - status: `404` if there are currently no sectors in the DB or the selected sector does not exist or is not linked with the selected expert
- DELETE `/API/experts/{id}`
  - Request parameters: ID of the desired expert
  - Request body: none
  - Result:
    - status: `200`
      - body: `1`
    - status: `404`  if no expert is found with the passed expertId
- GET `/API/experts/sectors`
  - Request parameters: none
  - Request body: none
  - Result:
    - status: `200`
      - body: `List<ExpertDTO>`
    - status: `404`  if there are currently no sectors in the DB
- GET `/API/experts/{id}/sectors`
  - Request parameters: ID of the desired expert
  - Request body: none
  - Result:
    - status: `200`
      - body: `List<ExpertDTO>`
    - status: `404`  if there are currently no sectors in the DB or there are no sectors associated with the selected expert
- Post `/API/experts/{id}/sectors`
  - Request parameters: ID of the desired expert
  - Request body: sectorDTO
  - Result:
    - status: `200`
      - body: true
    - status: `404`  if no expert is found with the passed expertId
    - status: `422` if the inserted input for the sector is not valid
- DELETE `/API/experts/{expertId}/sectors/{sectorId}`
  - Request parameters: ID of the desired expert, ID of the desired sector
  - Request body: none
  - Result:
    - status: `200`
      - body: `1`
    - status: `404`  if no expert is found with the passed expertId or there are currently no sectors in the DB or the selected sector does not exist or there are no sectors associated with the selected expert or the selected sector is not linked with the selected expert
- POST `/API/tickets/{ticketId}/messages`
  - Request parameters: none
  - Request body: 'multipart/form-data' request like {attachments: [list of files], fromUser : ['False' or 'True'], text: 'string...'}
  - Result:
    - status: `201`
      - body: `Long`
    - status: `404`
      - body: `Ticket non existant`
    - status: `405`
      - body: `Media entity not processable`

- GET `/API/tickets/{ticketId}/messages`
  - Request parameters: none
  - Request body: none
  - Result:
    - status: `200`
      - body: `List<MessageDTO>`
    - status: `404`
      - body: `Ticket non existant`
- GET `/API/tickets/{ticketId}/history`
  - Request parameters: none
  - Request body: none
  - Result:
    - status: `200`
      - body: `List<TicketHistoryDTO>`
    - status: `404`
      - body: `Ticket non existant`