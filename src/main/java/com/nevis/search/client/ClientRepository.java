package com.nevis.search.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    @Query(value = """
            SELECT * FROM clients c
            WHERE c.email ILIKE '%' || :q || '%'
               OR (c.first_name || ' ' || c.last_name) ILIKE '%' || :q || '%'
               OR c.description ILIKE '%' || :q || '%'
            LIMIT :limit
            """, nativeQuery = true)
    List<Client> searchByText(@Param("q") String query, @Param("limit") int limit);
}
