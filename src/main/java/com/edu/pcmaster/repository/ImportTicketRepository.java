package com.edu.pcmaster.repository;

import com.edu.pcmaster.entity.ImportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImportTicketRepository extends JpaRepository<ImportTicket, UUID> {
}
