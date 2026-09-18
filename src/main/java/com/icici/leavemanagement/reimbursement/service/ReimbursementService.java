package com.example.reimbursementservice.service;



import com.example.reimbursementservice.entity.Reimbursement;
import com.example.reimbursementservice.repository.ReimbursementRepository;
import org.springframework.stereotype.Service;
import com.example.reimbursementservice.exception.ReimbursementNotFoundException;
import java.util.List;
import java.util.Optional;

@Service
public class ReimbursementService {

    private final ReimbursementRepository reimbursementRepository;

    // Constructor injection
    public ReimbursementService(ReimbursementRepository reimbursementRepository) {
        this.reimbursementRepository = reimbursementRepository;
    }

    // CREATE
    public Reimbursement createReimbursement(Reimbursement reimbursement) {
        return reimbursementRepository.save(reimbursement);
    }

    // READ - Get all
    public List<Reimbursement> getAllReimbursements() {
        return reimbursementRepository.findAll();
    }

    // READ - Get by ID
    public Reimbursement getReimbursementById(Long id) {

    return reimbursementRepository.findById(id)
            .orElseThrow(() ->
                    new ReimbursementNotFoundException(
                            "Reimbursement with ID " + id + " not found"
                    ));
}

    // UPDATE
    public Reimbursement updateReimbursement(Long id, Reimbursement updatedReimbursement) {

        Reimbursement existingReimbursement =
        reimbursementRepository.findById(id)
        .orElseThrow(() ->
                new ReimbursementNotFoundException(
                        "Reimbursement with ID " + id + " not found"
                ));

        existingReimbursement.setEmployeeId(updatedReimbursement.getEmployeeId());
        existingReimbursement.setEmployeeName(updatedReimbursement.getEmployeeName());
        existingReimbursement.setTravelType(updatedReimbursement.getTravelType());
        existingReimbursement.setSource(updatedReimbursement.getSource());
        existingReimbursement.setDestination(updatedReimbursement.getDestination());
        existingReimbursement.setAmount(updatedReimbursement.getAmount());
        existingReimbursement.setStatus(updatedReimbursement.getStatus());

        return reimbursementRepository.save(existingReimbursement);
    }

    // DELETE
    public void deleteReimbursement(Long id) {
        reimbursementRepository.deleteById(id);
    }
}