package com.smartcampusmng.campusmanager.repository;

import com.smartcampusmng.campusmanager.entity.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeeTypeRepository extends JpaRepository<FeeType, Long> {
} 