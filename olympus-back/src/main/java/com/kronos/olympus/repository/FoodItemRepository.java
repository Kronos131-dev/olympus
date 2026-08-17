package com.kronos.olympus.repository;

import com.kronos.olympus.model.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.kronos.olympus.model.enums.FoodSource;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    // Pour vérifier si l'aliment est déjà dans notre base locale (Fallback cache) via le scan
    Optional<FoodItem> findByBarcode(String barcode);
    boolean existsBySource(FoodSource source);

    // L'import CIQUAL est rejouable : il retrouve chaque aliment par son alim_code ANSES et met
    // sa ligne à jour, plutôt que d'en créer une seconde.
    List<FoodItem> findByCiqualCodeIsNotNull();
    
    // Recherche d'aliments par nom avec tri par longueur pour avoir les correspondances exactes en premier
    @Query("SELECT f FROM FoodItem f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY LENGTH(f.name) ASC")
    List<FoodItem> searchByNameOrderedByLength(@Param("name") String name);

    // Spring Data JPA n'aime pas les apostrophes dans les commentaires des requêtes multi-lignes
    @Query(value = "SELECT * FROM food_items WHERE source = :source AND (to_tsvector('french', unaccent(name)) @@ websearch_to_tsquery('french', unaccent(:query)) OR similarity(unaccent(name), unaccent(:query)) > 0.25) ORDER BY similarity(unaccent(name), unaccent(:query)) DESC, CASE WHEN unaccent(name) ILIKE unaccent(:query) || '%' THEN 0 ELSE 1 END, length(name) ASC", nativeQuery = true)
    List<FoodItem> searchSmartCiqual(@Param("query") String query, @Param("source") String source);
}