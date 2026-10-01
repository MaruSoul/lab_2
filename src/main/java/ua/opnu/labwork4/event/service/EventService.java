package ua.opnu.labwork4.event.service;

import org.springframework.stereotype.Service;
import ua.opnu.labwork4.category.model.Category;
import ua.opnu.labwork4.category.repository.CategoryRepository;
import ua.opnu.labwork4.event.model.Event;
import ua.opnu.labwork4.event.repository.EventRepository;
import ua.opnu.labwork4.exception.BadRequestException;
import ua.opnu.labwork4.exception.ConflictOperationException;
import ua.opnu.labwork4.exception.ResourceNotFoundException;
import ua.opnu.labwork4.organizer.model.Organizer;
import ua.opnu.labwork4.organizer.repository.OrganizerRepository;
import ua.opnu.labwork4.participant.model.Participant;
import ua.opnu.labwork4.participant.repository.ParticipantRepository;
import ua.opnu.labwork4.registration.repository.RegistrationRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipantRepository participantRepository;
    private final OrganizerRepository organizerRepository;
    private final RegistrationRepository registrationRepository;

    public EventService(EventRepository eventRepository, CategoryRepository categoryRepository, ParticipantRepository participantRepository, OrganizerRepository organizerRepository, RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.participantRepository = participantRepository;
        this.organizerRepository = organizerRepository;
        this.registrationRepository = registrationRepository;
    }

    public Event createEvent(Event event) {
        // дати повинні відповідати логіці предметної області
        if (event.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Дата події не може бути у минулому");
        }

        // не можна створювати запис, якщо пов’язана сутність не існує
        if (event.getOrganizer() != null) {
            Organizer org = organizerRepository.findById(event.getOrganizer().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Організатора з таким ID не існує"));
            event.setOrganizer(org);
        }

        return eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Подію з ID " + id + " не знайдено"));
    }

    public Event updateEvent(Long id, Event updatedEvent) {
        Event event = getEventById(id);

        if (updatedEvent.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Дата події не може бути у минулому");
        }

        if (updatedEvent.getOrganizer() != null) {
            Organizer org = organizerRepository.findById(updatedEvent.getOrganizer().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Організатора з таким ID не існує"));
            event.setOrganizer(org);
        }

        event.setTitle(updatedEvent.getTitle());
        event.setDescription(updatedEvent.getDescription());
        event.setDate(updatedEvent.getDate());
        event.setLocation(updatedEvent.getLocation());
        return eventRepository.save(event);
    }

    public void deleteEvent(Long id) {
        Event event = getEventById(id);

        if (!registrationRepository.findByEventId(id).isEmpty()) {
            throw new ConflictOperationException("Не можна видалити подію, на яку є активні реєстрації");
        }

        eventRepository.delete(event);
    }

    public List<Event> getEventsByCategory(Long categoryId) {
        return eventRepository.findByCategoriesId(categoryId);
    }

    public List<Participant> getEventParticipants(Long eventId) {
        return participantRepository.findByEventsId(eventId);
    }

    public Event addCategoryToEvent(Long eventId, Long categoryId) {
        Event event = getEventById(eventId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Категорію з ID " + categoryId + " не знайдено"));

        if (!event.getCategories().contains(category)) {
            event.getCategories().add(category);
            return eventRepository.save(event);
        }
        return event;
    }

    // видалення категорії з події
    public Event removeCategoryFromEvent(Long eventId, Long categoryId) {
        Event event = getEventById(eventId);
        event.getCategories().removeIf(c -> c.getId().equals(categoryId));
        return eventRepository.save(event);
    }

    // аошук подій
    public List<Event> searchEvents(String query) {
        if (query == null || query.isBlank()) return getAllEvents();
        return eventRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
    }

    public List<Event> advancedSearch(String query, String dateStr, String location) {
        query = query == null ? "" : query;
        location = location == null ? "" : location;

        // дата - необов'язковий фільтр: якщо її не передано, шукаємо лише за назвою та локацією.
        // Spring Data перетворює null-параметр на умову "date IS NULL", тому передавати null у запит з датою не можна
        if (dateStr == null || dateStr.isBlank()) {
            return eventRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCase(query, location);
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Некоректний формат дати: '" + dateStr + "'. Очікується формат YYYY-MM-DD (наприклад, 2026-04-17)");
        }
        return eventRepository.findByTitleContainingIgnoreCaseAndDateAndLocationContainingIgnoreCase(query, date, location);
    }
}