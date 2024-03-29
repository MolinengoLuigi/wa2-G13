package it.polito.wa2.g13.server

import it.polito.wa2.g13.server.jwtAuth.AuthService
import it.polito.wa2.g13.server.jwtAuth.JwtResponse
import it.polito.wa2.g13.server.jwtAuth.LoginDTO
import it.polito.wa2.g13.server.jwtAuth.RegisterDTO
import it.polito.wa2.g13.server.ticketing.experts.*
import it.polito.wa2.g13.server.ticketing.sectors.Sector
import it.polito.wa2.g13.server.ticketing.sectors.SectorDTO
import it.polito.wa2.g13.server.ticketing.sectors.SectorRepository
import it.polito.wa2.g13.server.ticketing.sectors.toDTO
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URI


@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExpertAndSectorTests {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:latest")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        }
    }

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var expertRepository: ExpertRepository

    @Autowired
    lateinit var sectorRepository: SectorRepository

    @Autowired
    lateinit var authService: AuthService

    data class CreateExpertData(val dto: ExpertDTO?, val result: ResponseEntity<String>, val headers: HttpHeaders)

    fun createExpert(
        token: String,
        username: String = "test-expert",
        email: String = "will@gmail.com"
    ): CreateExpertData {
        val baseUrl = "http://localhost:$port/API/createExpert"
        val uri = URI(baseUrl)
        val regDTO =
            RegisterDTO(username = username, password = "password", email = email, name = "Will", surname = "Hunting")

        val headers = HttpHeaders()
        headers.setBearerAuth(token)
        headers.set("X-COM-PERSIST", "true")

        var request = HttpEntity(regDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        var expertDTO = expertRepository.findAll().find { it.username == username && it.email == email }?.toDTO()
        return CreateExpertData(expertDTO, result, headers)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertSetSuccessTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        var token = authService.login(loginDTO)?.jwtAccessToken

        Assertions.assertNotEquals(null, token)

        val postDTO = RegisterDTO(
            username = "test-expert",
            password = "password",
            email = "will@gmail.com",
            name = "Will",
            surname = "Hunting"
        )

        val res = createExpert(token!!)

        Assertions.assertEquals(HttpStatus.CREATED, res.result.statusCode)
        Assertions.assertEquals(
            RegisterDTO(postDTO.username, "", postDTO.email, postDTO.name, postDTO.surname),
            res.dto?.toRegisterDTO()
        )

        val deleteExpertUrl = "http://localhost:$port/API/experts/${res.dto?.id}"

        val deleteResult = restTemplate.exchange(
            deleteExpertUrl,
            HttpMethod.DELETE,
            HttpEntity<Object>(res.headers),
            String::class.java
        )

        Assertions.assertEquals(HttpStatus.OK, deleteResult.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertSetConflictTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        var token = authService.login(loginDTO)?.jwtAccessToken

        Assertions.assertNotEquals(null, token)

        val baseUrl = "http://localhost:$port/API/createExpert"
        val uri = URI(baseUrl)

        val originalRes = createExpert(token!!)
        Assertions.assertEquals(HttpStatus.CREATED, originalRes.result.statusCode)

        val postDTO = RegisterDTO(
            username = "test-expert-2",
            password = "password",
            email = "will@gmail.com",
            name = "Will",
            surname = "Hunting"
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")
        val request = HttpEntity(postDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        Assertions.assertEquals(HttpStatus.CONFLICT, result.statusCode)
        /*Assertions.assertEquals(
            "\"detail\":\"An expert with this email address already exists!\"",
            result.body.toString().split(",")[3]
        )*/

        val deleteExpertUrl = "http://localhost:$port/API/experts/${originalRes.dto?.id}"

        val deleteResult =
            restTemplate.exchange(deleteExpertUrl, HttpMethod.DELETE, HttpEntity<Object>(headers), String::class.java)

        Assertions.assertEquals(HttpStatus.OK, deleteResult.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertSetUnprocessableEntityTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        var token = authService.login(loginDTO)?.jwtAccessToken

        Assertions.assertNotEquals(null, token)

        val baseUrl = "http://localhost:$port/API/createExpert"
        val uri = URI(baseUrl)
        val postDTO = RegisterDTO(
            username = "test-expert-2",
            password = "testpwd",
            email = "will@ gmail.com",
            name = "Will",
            surname = "Hunting"
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(postDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"The inserted input is not valid!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertSetUnauthorizedTest1() {
        val loginDTO = LoginDTO(username = "expert", password = "password") //it is an expert
        var token = authService.login(loginDTO)?.jwtAccessToken

        Assertions.assertNotEquals(null, token)
        val baseUrl = "http://localhost:$port/API/createExpert"
        val uri = URI(baseUrl)
        val postDTO = RegisterDTO(
            username = "test-expert-2",
            password = "testpwd",
            email = "will@gmail.com",
            name = "Will",
            surname = "Hunting"
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(postDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertSetUnauthorizedTest2() {
        val loginDTO = LoginDTO(username = "client", password = "password") //it is a client
        var token = authService.login(loginDTO)?.jwtAccessToken

        Assertions.assertNotEquals(null, token)
        val baseUrl = "http://localhost:$port/API/createExpert"
        val uri = URI(baseUrl)
        val postDTO = RegisterDTO(
            username = "test-expert-2",
            password = "testpwd",
            email = "will@gmail.com",
            name = "Will",
            surname = "Hunting"
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(postDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertSetUnauthorizedTest3() {
        val baseUrl = "http://localhost:$port/API/createExpert"
        val uri = URI(baseUrl)
        val postDTO = RegisterDTO(
            username = "test-expert-2",
            password = "testpwd",
            email = "will@gmail.com",
            name = "Will",
            surname = "Hunting"
        )

        val headers = HttpHeaders()
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(postDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }


    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertGetSuccess() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is an expert
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val res = createExpert(token!!)
        val baseUrl = "http://localhost:$port/API/experts/${res.dto?.id}"
        val uri = URI(baseUrl)

        val result = restTemplate.exchange(uri, HttpMethod.GET, HttpEntity<Object>(res.headers), String::class.java)

        Assertions.assertEquals(HttpStatus.OK, result.statusCode)

        val deleteExpertUrl = "http://localhost:$port/API/experts/${res.dto?.id}"

        val deleteResult = restTemplate.exchange(
            deleteExpertUrl,
            HttpMethod.DELETE,
            HttpEntity<Object>(res.headers),
            String::class.java
        )

        Assertions.assertEquals(HttpStatus.OK, deleteResult.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertGetNotFound() {
        val loginDTO = LoginDTO(username = "expert", password = "password") //it is an expert
        var token = authService.login(loginDTO)?.jwtAccessToken

        Assertions.assertNotEquals(null, token)

        val baseUrl = "http://localhost:$port/API/experts/aaaaaa-aaaaa-aaaaa-aaaaa"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val result = restTemplate.exchange(uri, HttpMethod.GET, HttpEntity<Object>(headers), String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"Expert Not Found!\"",
            result.body.toString().split(",")[3]
        )
    }


    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertModifySuccessTest1() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //It is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val res = createExpert(token!!)
        Assertions.assertEquals(HttpStatus.CREATED, res.result.statusCode)
        val baseUrl = "http://localhost:$port/API/experts/${res.dto?.id}"
        val uri = URI(baseUrl)
        val expertDTO = ExpertDTO(
            id = res.dto!!.id,
            username = res.dto!!.username,
            name = res.dto!!.name,
            surname = "News",
            email = res.dto!!.email
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(expertDTO, headers)

        val result = restTemplate.exchange(uri, HttpMethod.PUT, request, String::class.java)

        Assertions.assertEquals(HttpStatus.OK, result.statusCode)
        val expert = expertRepository.findByIdOrNull(res.dto!!.id)
        Assertions.assertEquals("Will", expert?.name)
        Assertions.assertEquals("News", expert?.surname)
        val deleteExpertUrl = "http://localhost:$port/API/experts/${res.dto?.id}"

        val deleteResult = restTemplate.exchange(
            deleteExpertUrl,
            HttpMethod.DELETE,
            HttpEntity<Object>(res.headers),
            String::class.java
        )

        Assertions.assertEquals(HttpStatus.OK, deleteResult.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertModifySuccessTest2() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //It is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val res = createExpert(token!!)
        Assertions.assertEquals(HttpStatus.CREATED, res.result.statusCode)
        val baseUrl = "http://localhost:$port/API/experts/${res.dto?.id}"
        val uri = URI(baseUrl)
        val expertDTO = ExpertDTO(
            id = res.dto!!.id,
            username = res.dto!!.username,
            name = res.dto!!.name,
            surname = res.dto!!.surname,
            email = "william@gmail.com"
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(expertDTO, headers)

        val result = restTemplate.exchange(uri, HttpMethod.PUT, request, String::class.java)

        Assertions.assertEquals(HttpStatus.OK, result.statusCode)
        Assertions.assertEquals("william@gmail.com", expertRepository.findByIdOrNull(res.dto!!.id)?.email)

        val deleteExpertUrl = "http://localhost:$port/API/experts/${res.dto?.id}"

        val deleteResult = restTemplate.exchange(
            deleteExpertUrl,
            HttpMethod.DELETE,
            HttpEntity<Object>(res.headers),
            String::class.java
        )

        Assertions.assertEquals(HttpStatus.OK, deleteResult.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertModifyUnprocessableEntityTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //It is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val res = createExpert(token!!)
        Assertions.assertEquals(HttpStatus.CREATED, res.result.statusCode)
        val baseUrl = "http://localhost:$port/API/experts/${res.dto?.id}"
        val uri = URI(baseUrl)
        val expertDTO = ExpertDTO(
            id = res.dto!!.id,
            username = res.dto!!.username,
            name = res.dto!!.name,
            surname = res.dto!!.surname,
            email = "william @gmail.com"
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(expertDTO, headers)

        val result = restTemplate.exchange(uri, HttpMethod.PUT, request, String::class.java)

        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"The inserted input for the expert is not valid!\"",
            result.body.toString().split(",")[3]
        )
        val deleteExpertUrl = "http://localhost:$port/API/experts/${res.dto?.id}"

        val deleteResult = restTemplate.exchange(
            deleteExpertUrl,
            HttpMethod.DELETE,
            HttpEntity<Object>(res.headers),
            String::class.java
        )

        Assertions.assertEquals(HttpStatus.OK, deleteResult.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertModifyNotFoundTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //It is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val baseUrl = "http://localhost:$port/API/experts/aaaaaaa"
        val uri = URI(baseUrl)
        val expertDTO = ExpertDTO(
            id = "aaaaaaa",
            username = "will",
            name = "William",
            surname = "Hunt",
            email = "william@gmail.com"
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(expertDTO, headers)

        val result = restTemplate.exchange(uri, HttpMethod.PUT, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"Expert Not Found!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertModifyConflictTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //It is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val res = createExpert(token!!)
        Assertions.assertEquals(HttpStatus.CREATED, res.result.statusCode)
        val resNew = createExpert(token!!, username = "text-expert2", email = "will-2@gmail.com")
        Assertions.assertEquals(HttpStatus.CREATED, resNew.result.statusCode)
        val baseUrl = "http://localhost:$port/API/experts/${resNew.dto?.id}"
        val uri = URI(baseUrl)
        val expertDTO = ExpertDTO(
            id = resNew.dto!!.id,
            username = resNew.dto!!.username,
            name = resNew.dto!!.name,
            surname = resNew.dto!!.surname,
            email = res.dto!!.email
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(expertDTO, headers)

        val result = restTemplate.exchange(uri, HttpMethod.PUT, request, String::class.java)

        Assertions.assertEquals(HttpStatus.CONFLICT, result.statusCode)

        var deleteExpertUrl = "http://localhost:$port/API/experts/${res.dto?.id}"

        var deleteResult = restTemplate.exchange(
            deleteExpertUrl,
            HttpMethod.DELETE,
            HttpEntity<Object>(res.headers),
            String::class.java
        )

        Assertions.assertEquals(HttpStatus.OK, deleteResult.statusCode)

        deleteExpertUrl = "http://localhost:$port/API/experts/${resNew.dto?.id}"

        deleteResult = restTemplate.exchange(
            deleteExpertUrl,
            HttpMethod.DELETE,
            HttpEntity<Object>(res.headers),
            String::class.java
        )

        Assertions.assertEquals(HttpStatus.OK, deleteResult.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertModifyUnauthorizedTest1() {
        val loginDTO = LoginDTO(username = "expert", password = "password") //It is an expert
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val baseUrl = "http://localhost:$port/API/experts/aaaaaaa"
        val uri = URI(baseUrl)
        val expertDTO = ExpertDTO(
            id = "aaaaaaa",
            username = "will",
            name = "William",
            surname = "Hunt",
            email = "william@gmail.com"
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(expertDTO, headers)

        val result = restTemplate.exchange(uri, HttpMethod.PUT, request, String::class.java)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertModifyUnauthorizedTest2() {
        val loginDTO = LoginDTO(username = "client", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val baseUrl = "http://localhost:$port/API/experts/aaaaaaa"
        val uri = URI(baseUrl)
        val expertDTO = ExpertDTO(
            id = "aaaaaaa",
            username = "will",
            name = "William",
            surname = "Hunt",
            email = "william@gmail.com"
        )

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(expertDTO, headers)

        val result = restTemplate.exchange(uri, HttpMethod.PUT, request, String::class.java)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertModifyUnauthorizedTest3() {
        val baseUrl = "http://localhost:$port/API/experts/aaaaaaa"
        val uri = URI(baseUrl)
        val expertDTO = ExpertDTO(
            id = "aaaaaaa",
            username = "will",
            name = "William",
            surname = "Hunt",
            email = "william@gmail.com"
        )

        val headers = HttpHeaders()
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(expertDTO, headers)

        val result = restTemplate.exchange(uri, HttpMethod.PUT, request, String::class.java)

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertDeleteNotFoundTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val baseUrl = "http://localhost:$port/API/experts/aaaaaa"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, HttpEntity<Object>(headers), String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"Expert Not Found!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertDeleteUnauthorizedTest1() {
        val loginDTO = LoginDTO(username = "expert", password = "password") //It is a expert
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val baseUrl = "http://localhost:$port/API/experts/aaaaaa"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, HttpEntity<Object>(headers), String::class.java)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertDeleteUnauthorizedTest2() {
        val loginDTO = LoginDTO(username = "client", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)
        val baseUrl = "http://localhost:$port/API/experts/aaaaaa"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, HttpEntity<Object>(headers), String::class.java)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertDeleteUnauthorizedTest3() {
        val baseUrl = "http://localhost:$port/API/experts/aaaaa"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.set("X-COM-PERSIST", "true")

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, HttpEntity<Object>(headers), String::class.java)

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertsBySectorGetSuccessTest() {
        val loginDTO = LoginDTO(username = "expert", password = "password") //It is an expert
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector = Sector(name = "linux")
        expert1.addSector(sector)
        expert2.addSector(sector)
        sector.addExpert(expert1)
        sector.addExpert(expert2)
        sectorRepository.save(sector)
        expertRepository.save(expert1)
        expertRepository.save(expert2)

        val baseUrl = "http://localhost:$port/API/experts/?sectorName=LINUX"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.GET, request, String::class.java)

        Assertions.assertEquals(HttpStatus.OK, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertsBySectorGetSectorsNotFoundTest() {
        val loginDTO = LoginDTO(username = "client", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)

        val baseUrl = "http://localhost:$port/API/experts/?sectorName=LINUX"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.GET, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"There are currently no sectors in the DB!\"",
            result.body.toString().split(",")[3]
        )
    }


    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertsBySectorGetSectorNotFoundTest() {
        val loginDTO = LoginDTO(username = "client", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector = Sector(name = "Hardware")
        expert1.addSector(sector)
        sector.addExpert(expert1)
        sectorRepository.save(sector)
        expertRepository.save(expert1)

        val baseUrl = "http://localhost:$port/API/experts/?sectorName=LINUX"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.GET, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"The selected sector does not exist!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun expertsBySectorGetExpertOfSelectedSectorNotFoundTest() {
        val loginDTO = LoginDTO(username = "client", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector = Sector(name = "linux")
        sectorRepository.save(sector)

        val baseUrl = "http://localhost:$port/API/experts/?sectorName=LINUX"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.GET, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"There are no experts associated with the selected sector!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorsAllGetSuccessTest() {
        val loginDTO = LoginDTO(username = "client", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        expert1.addSector(sector2)
        sector1.addExpert(expert1)
        sector2.addExpert(expert1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)

        val baseUrl = "http://localhost:$port/API/experts/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.GET, request, String::class.java)

        Assertions.assertEquals(HttpStatus.OK, result.statusCode)
        Assertions.assertEquals(
            "[{\"sectorId\":1,\"name\":\"linux\"},{\"sectorId\":2,\"name\":\"wi-fi\"}]",
            result.body
        )
    }


    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorsAllGetNotFoundTest() {
        val loginDTO = LoginDTO(username = "client", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)

        val baseUrl = "http://localhost:$port/API/experts/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.GET, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"There are currently no sectors in the DB!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorsOfExpertGetSuccessTest() {
        val loginDTO = LoginDTO(username = "client", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        expert1.addSector(sector2)
        sector1.addExpert(expert1)
        sector2.addExpert(expert1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)

        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.GET, request, String::class.java)

        Assertions.assertEquals(HttpStatus.OK, result.statusCode)
        Assertions.assertEquals(
            "[{\"sectorId\":1,\"name\":\"linux\"},{\"sectorId\":2,\"name\":\"wi-fi\"}]",
            result.body
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorsOfExpertGetExpertNotFoundTest() {
        val loginDTO = LoginDTO(username = "client", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        expert1.addSector(sector2)
        sector1.addExpert(expert1)
        sector2.addExpert(expert1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)

        val baseUrl = "http://localhost:$port/API/experts/cccc/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.GET, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"Expert Not Found!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorsOfExpertGetExpertSectorsNotFoundTest() {
        val loginDTO = LoginDTO(username = "client", password = "password") //It is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        expert1.addSector(sector2)
        sector1.addExpert(expert1)
        sector2.addExpert(expert1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)

        val baseUrl = "http://localhost:$port/API/experts/bbbb/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.GET, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"There are no sectors associated with the selected expert!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertSetSuccessTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        expertRepository.save(expert1)
        val sectorDTO = SectorDTO(sectorId = 1, name = "linux")
        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(sectorDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        Assertions.assertEquals(HttpStatus.OK, result.statusCode)
        Assertions.assertEquals(sectorDTO, sectorRepository.findByIdOrNull(1)?.toDTO())

    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertSetUnprocessableEntityTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        expertRepository.save(expert1)
        val sectorDTO = SectorDTO(sectorId = 1, name = "")
        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(sectorDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"The inserted input for the sector is not valid!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertSetExpertNotFoundTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        expertRepository.save(expert1)
        val sectorDTO = SectorDTO(sectorId = 1, name = "cellular network")
        val baseUrl = "http://localhost:$port/API/experts/bbbb/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(sectorDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"Expert Not Found!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertSetExpertUnauthorizedTest1() {
        val loginDTO = LoginDTO(username = "client", password = "password")
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        expertRepository.save(expert1)
        val sectorDTO = SectorDTO(sectorId = 1, name = "cellular network")
        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(sectorDTO, headers)

        val result = restTemplate.exchange(uri, HttpMethod.POST, request, String::class.java)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertSetExpertUnauthorizedTest2() {
        val loginDTO = LoginDTO(username = "client", password = "password") //it is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        expertRepository.save(expert1)
        val sectorDTO = SectorDTO(sectorId = 1, name = "cellular network")
        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(sectorDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertSetExpertUnauthorizedTest3() {
        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        expertRepository.save(expert1)
        val sectorDTO = SectorDTO(sectorId = 1, name = "cellular network")
        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(sectorDTO, headers)

        val result = restTemplate.postForEntity(uri, request, String::class.java)

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertDeleteSuccessTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        expert1.addSector(sector2)
        sector1.addExpert(expert1)
        sector2.addExpert(expert1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)
        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors/1"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, request, String::class.java)

        Assertions.assertEquals(HttpStatus.OK, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertDeleteExpertNotFoundTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        expert1.addSector(sector2)
        sector1.addExpert(expert1)
        sector2.addExpert(expert1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)
        val baseUrl = "http://localhost:$port/API/experts/cccc/sectors/1"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"Expert Not Found!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertDeleteSectorsNotFoundTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)

        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors/1"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"There are currently no sectors in the DB!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertDeleteSectorNotFoundTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)

        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors/3"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"The selected sector does not exist!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertDeleteExpertSectorsNotFoundTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)

        val baseUrl = "http://localhost:$port/API/experts/bbbb/sectors/1"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"There are no sectors associated with the selected expert!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertDeleteExpertSectorNotFoundTest() {
        val loginDTO = LoginDTO(username = "manager", password = "password") //it is a manager
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)

        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors/2"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, request, String::class.java)

        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        Assertions.assertEquals(
            "\"detail\":\"The selected sector is not linked with the selected expert!\"",
            result.body.toString().split(",")[3]
        )
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertDeleteUnauthorizedTest1() {
        val loginDTO = LoginDTO(username = "expert", password = "password") //it is an expert
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        expert1.addSector(sector2)
        sector1.addExpert(expert1)
        sector2.addExpert(expert1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)
        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors/1"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, request, String::class.java)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertDeleteUnauthorizedTest2() {
        val loginDTO = LoginDTO(username = "client", password = "password") //it is a client
        val token = authService.login(loginDTO)?.jwtAccessToken
        Assertions.assertNotEquals(null, token)

        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        expert1.addSector(sector2)
        sector1.addExpert(expert1)
        sector2.addExpert(expert1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)
        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors/1"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.setBearerAuth(token!!)
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, request, String::class.java)

        Assertions.assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    fun sectorForExpertDeleteUnauthorizedTest3() {
        val expert1 =
            Expert(id = "aaaa", username = "aaaa", email = "aaaa@gmail.com", name = "William", surname = "Hunt")
        val expert2 =
            Expert(id = "bbbb", username = "bbbb", email = "bbbb@gmail.com", name = "Matthew", surname = "Luck")
        expertRepository.save(expert1)
        expertRepository.save(expert2)
        val sector1 = Sector(name = "linux")
        val sector2 = Sector(name = "wi-fi")
        expert1.addSector(sector1)
        expert1.addSector(sector2)
        sector1.addExpert(expert1)
        sector2.addExpert(expert1)
        sectorRepository.save(sector1)
        sectorRepository.save(sector2)
        expertRepository.save(expert1)
        val baseUrl = "http://localhost:$port/API/experts/aaaa/sectors/1"
        val uri = URI(baseUrl)

        val headers = HttpHeaders()
        headers.set("X-COM-PERSIST", "true")

        val request = HttpEntity(null, headers)

        val result = restTemplate.exchange(uri, HttpMethod.DELETE, request, String::class.java)

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
    }
}


