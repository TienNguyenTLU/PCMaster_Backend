package com.edu.pcmaster.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.PcBuild;
import com.edu.pcmaster.models.User;

public interface PcBuildRepository extends JpaRepository<PcBuild, Long> {
	List<PcBuild> findByUser(User user);
}

