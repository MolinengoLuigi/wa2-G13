package it.polito.wa2.g13.server.ticketing.experts

import it.polito.wa2.g13.server.ticketing.sectors.Sector
import jakarta.persistence.*

@Entity
@Table(name= "experts")
class Expert(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
        generator = "expert_generator")
    @SequenceGenerator(name="expert_generator",
        sequenceName = "expert_seq",
        initialValue = 1,
        allocationSize = 1)
    var expertId: Long,
    var name: String,
    var surname: String,
    var email: String) {

    @ManyToMany
    @JoinTable(name="expert_sector",
        joinColumns = [JoinColumn(name="expert_id")],
        inverseJoinColumns = [JoinColumn(name="sector_id")]
    )
    val sectors: MutableSet<Sector> = mutableSetOf()

    fun addSector(s: Sector) {
        sectors.add(s)
        s.experts.add(this)
    }

    fun removeSector(s: Sector){
        sectors.remove(s)
        s.experts.remove(this)
    }

}

fun ExpertDTO.toExpert(): Expert {
    return Expert(expertId, name, surname, email)
}

fun ExpertDTO.toExpertWithId(id: Long) : Expert{
    return Expert(id, name, surname, email)
}