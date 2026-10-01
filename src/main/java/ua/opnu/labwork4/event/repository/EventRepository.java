package ua.opnu.labwork4.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.opnu.labwork4.event.model.Event;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // events/category/{categoryId}
    List<Event> findByCategoriesId(Long categoryId);

    // /organizers/{id}/events
    List<Event> findByOrganizerId(Long organizerId);

    // /participants/{id}/events
    List<Event> findByParticipantsId(Long participantId);

    // /search/events?query=
    List<Event> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    // /search/events/advanced?query=&date=&location=
    List<Event> findByTitleContainingIgnoreCaseAndDateAndLocationContainingIgnoreCase(String title, LocalDate date, String location);

    // /search/events/advanced?query=&location= (без фільтра за датою)
    List<Event> findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCase(String title, String location);

    long countByDateAfter(LocalDate date);
    long countByDateBefore(LocalDate date);
}