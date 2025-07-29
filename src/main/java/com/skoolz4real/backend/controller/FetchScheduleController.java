package com.skoolz4real.backend.controller;

import com.skoolz4real.backend.service.ScheduleFetcher;
import com.skoolz4real.backend.service.ScheduleParser;
import com.skoolz4real.backend.model.Course;
import com.skoolz4real.backend.model.StudentCredentials;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FetchScheduleController {

    @PostMapping("/fetch-schedule")
    public List<Course> loginAndFetchSchedule(@RequestBody StudentCredentials studentCredentials) {
        String html = ScheduleFetcher.fetchSchedule(studentCredentials.getUsername(), studentCredentials.getPassword());
        return ScheduleParser.parse(html);
    }
}
