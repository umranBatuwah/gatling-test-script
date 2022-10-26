package com.synectiks.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.synectiks.asset.domain.time;

@SuppressWarnings("unused")
@Repository
public interface TimeRepository extends JpaRepository<time,Long>{
    
}
