package com.postoffice.config;

import com.postoffice.model.PostalService;
import com.postoffice.repository.PostalServiceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements ApplicationRunner {

    private final PostalServiceRepository postalServiceRepository;

    public DataSeeder(PostalServiceRepository postalServiceRepository) {
        this.postalServiceRepository = postalServiceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("Normal", "normal_post", "5.00", 5760);
        seed("Speed", "speed_post", "20.00", 1440);
        seed("Superfast", "super_fast", "50.00", 10);
    }

    private void seed(String name, String slug, String charges, int minutes) {
        postalServiceRepository.findBySlug(slug).ifPresentOrElse(existing -> {
            existing.setName(name);
            existing.setCharges(new BigDecimal(charges));
            existing.setDeliveryMin(minutes);
            postalServiceRepository.save(existing);
        }, () -> {
            PostalService service = new PostalService();
            service.setName(name);
            service.setSlug(slug);
            service.setCharges(new BigDecimal(charges));
            service.setDeliveryMin(minutes);
            postalServiceRepository.save(service);
        });
    }
}
