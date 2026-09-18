package com.example.reimbursementservice.repository;

import com.example.reimbursementservice.entity.Reimbursement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementRepository
        extends JpaRepository<Reimbursement, Long> {

}