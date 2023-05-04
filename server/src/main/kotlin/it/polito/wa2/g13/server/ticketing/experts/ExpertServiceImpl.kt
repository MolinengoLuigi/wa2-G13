package it.polito.wa2.g13.server.ticketing.experts


import it.polito.wa2.g13.server.ticketing.sectors.SectorRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ExpertServiceImpl(
    private val expertRepository: ExpertRepository,
    private val sectorRepository: SectorRepository
) : ExpertService {

    override fun getExperts(): List<ExpertDTO> {
        return expertRepository.findAll().map{it.toDTO()}
    }

    override fun setExpert(expertDTO: ExpertDTO): Boolean {

        val expert= expertDTO.toExpert()
        return if (!expertRepository.existsByEmail(expert.email)){
            expertRepository.save(expert)
            true
        }else false

    }

    override fun getExpertById(id: Long): ExpertDTO?{
        return expertRepository.findByIdOrNull(id)?.toDTO()
    }

    override fun modifyExpert(id: Long, expertDTO: ExpertDTO): Int {
        return if(expertRepository.existsById(id)){
            if(expertRepository.existsByEmailAndIdNot(expertDTO.email, id)){
                0 // DuplicateExpertException
            }else{
                expertRepository.save(expertDTO.toExpertWithId(id))
                1 // Ok
            }
        }else{
            2 // ExpertNotFoundException
        }
    }

    override fun getExpertsBySector(sectorName: String): List<ExpertDTO>? {

        val sector= sectorRepository.findByName(sectorName)
        val listOfExpertDTOs=
            expertRepository.findExpertsBySectors(sector).map{e -> e.toDTO()}
        return listOfExpertDTOs.ifEmpty {
            null
        }

    }

    override fun deleteExpertById(id: Long){
        expertRepository.deleteById(id)
    }

}