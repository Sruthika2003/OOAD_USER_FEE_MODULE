package com.smartcampusmng.campusmanager.service;

import com.smartcampusmng.campusmanager.entity.FeeType;
import com.smartcampusmng.campusmanager.repository.FeeTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeeTypeService {
    private final FeeTypeRepository feeTypeRepository;

    @Autowired
    public FeeTypeService(FeeTypeRepository feeTypeRepository) {
        this.feeTypeRepository = feeTypeRepository;
    }

    public List<FeeType> getAllFeeTypes() {
        return feeTypeRepository.findAll();
    }

    public FeeType getFeeTypeById(Long id) {
        return feeTypeRepository.findById(id).orElse(null);
    }
} 