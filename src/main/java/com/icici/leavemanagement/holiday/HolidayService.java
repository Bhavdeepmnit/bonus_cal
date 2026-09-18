package com.icici.leavemanagement.holiday;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.icici.leavemanagement.common.exception.ConflictException;
import com.icici.leavemanagement.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Company holidays. Leave already applied for keeps the day count it was saved with;
 * holiday changes only affect new requests.
 */
@Service
@RequiredArgsConstructor
public class HolidayService {
    private final HolidayRepository repository;

    public List<Holiday> getByYear(int year) {
        return repository.findByHolidayDateBetweenOrderByHolidayDate(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    @Transactional
    public Holiday create(HolidayRequest req) {
        if (repository.existsByHolidayDate(req.getDate())) {
            throw new ConflictException("A holiday on " + req.getDate() + " already exists");
        }
        return repository.save(new Holiday(null, req.getDate(), req.getName()));
    }

    @Transactional
    public void delete(Long id) {
        Holiday holiday = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Holiday", id));
        repository.delete(holiday);
    }

    public Set<LocalDate> datesBetween(LocalDate from, LocalDate to) {
        return repository.findByHolidayDateBetweenOrderByHolidayDate(from, to).stream()
            .map(Holiday::getHolidayDate)
            .collect(Collectors.toSet());
    }
}
