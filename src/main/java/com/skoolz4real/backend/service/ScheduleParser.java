package com.skoolz4real.backend.service;

import com.skoolz4real.backend.model.Course;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class ScheduleParser {

    public static List<Course> parse(String html) {
        List<Course> schedule = new ArrayList<>();

        Document doc = Jsoup.parse(html);

        Element table = doc.selectFirst("table");
        Elements rows = table.select("tr.glbdatadark");

        for (Element row : rows) {
            Elements cols = row.select("td");


            Course course = new Course();

            if(cols.size() <12) continue;

            course.setCourseCode(cols.get(0).text().trim()); // Class Code
            course.setHours(cols.get(1).text().trim()); // Credit hours
            course.setTitle(cols.get(2).text().trim()); // Title of class

            Element emailLink = cols.get(3).select("a[href]").first(); // Email
            String instructor = cols.get(3).text().trim();
            if (emailLink != null) {
                instructor += ": " + emailLink.attr("href").replace("mailto:", "");
            }
            course.setInstructor(instructor); // Instructor

            course.setCampus(cols.get(4).text().trim()); // Campus
            course.setBuilding(cols.get(5).text().trim()); // Building
            course.setRoom(cols.get(6).text().trim()); // Room

            // Clean up the days field: replace '-' with an empty string or default value
            String days = cols.get(7).text().trim();
            course.setDays(days.replace("-", "").trim()); // Day of class

            course.setTime(cols.get(8).text().trim()); // Class time
            course.setDate(cols.get(9).text().trim()); // Period of semester (when starts and ends)
            course.setTm(cols.get(10).text().trim()); // Season
            course.setType(cols.get(11).text().trim()); // Type whether online or offline

            schedule.add(course);
        }

        return schedule;
    }
}
