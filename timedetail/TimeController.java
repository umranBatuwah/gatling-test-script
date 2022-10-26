package com.synectiks.asset.controller;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synectiks.asset.business.service.TimeService;
import com.synectiks.asset.domain.time;

import io.jsonwebtoken.io.IOException;


@RestController
@RequestMapping("/api")
public class TimeController {
    
    private static final Logger logger = LoggerFactory.getLogger(TimeController.class);

    @Autowired
    private TimeService timeService;

    @GetMapping("/time-detail/{id}")
	public ResponseEntity<time> gettimeDetail(@PathVariable Long id) {
		logger.info("Request to get time-detail. Id: "+id);
		Optional<time> odp = timeService.gettimeDetail(id);
		if(odp.isPresent()) {
			return ResponseEntity.status(HttpStatus.OK).body(odp.get());
		}
		return ResponseEntity.status(HttpStatus.OK).body(null);
	}

    @GetMapping("/time-detail")
	public ResponseEntity<List<time>> getAlltimeDetail() {
		logger.info("Request to get time-detail");
		List<time> list = timeService.getAlltimeDetail();
		return ResponseEntity.status(HttpStatus.OK).body(list);
	}


    @DeleteMapping("/time-detail/{id}")
	public ResponseEntity<Optional<time>> deletetimeDetail(@PathVariable Long id) {
		logger.info("Request to delete time-detail by id: {}", id);
		Optional<time> oSpa = timeService.deletetimeDetail(id);
		return ResponseEntity.status(HttpStatus.OK).body(oSpa);
	}
	
	@PostMapping("/time-detail")
	public ResponseEntity<time> createtimeDetail(@RequestBody time obj){
		logger.info("Request to create new time-detail");
		time spa = timeService.createtimeDetail(obj);
		return ResponseEntity.status(HttpStatus.OK).body(spa);
	}
	
	@PutMapping("/time-detail")
	public ResponseEntity<time> updatetimeDetail(@RequestBody time obj){
		logger.info("Request to update time-detail");
		time spa = timeService.updatetimeDetail(obj);
		return ResponseEntity.status(HttpStatus.OK).body(spa);
	}

	@GetMapping("/time-detail/transform")
	public ResponseEntity<Object> change() throws IOException {
		logger.info("Request to transform service-detail data");
		Object m =timeService.transformServiceDetailsListToTree();
		return ResponseEntity.status(HttpStatus.OK).body(m);
	}
}
