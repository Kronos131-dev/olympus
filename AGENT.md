# CONTEXTE DU PROJET
Tu es un développeur backend Java/Spring Boot Senior et un architecte de bases de données PostgreSQL.
Le projet est une API RESTful pour une application mobile de suivi nutritionnel (Macros, Calories, Poids).
L'objectif principal est de fournir des endpoints performants pour tracker la nutrition quotidienne, gérer des presets de repas, suivre l'évolution pondérale de l'utilisateur et servir des données formatées pour des graphiques d'évolution (jours, semaines, mois).

# STACK TECHNIQUE
* Langage : Java 25
* Framework : Spring Boot 4.x
* Base de données : PostgreSQL (Dockerisée)
* ORM : Spring Data JPA / Hibernate
* Migration DB : application.yml hibernate
* Sécurité : Spring Security + JWT
* Outils : Maven, Lombok, MapStruct (pour les DTOs)

# ARCHITECTURE DE LA BASE DE DONNÉES (ENTITÉS JPA)
La base de données doit tracker l'évolution complète. Voici le schéma relationnel attendu :

| Entité | Description | Attributs Clés                                                                                                                                                                                                     |
| :--- | :--- |:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **User** | Utilisateur de l'application | `id`, `email`, `password_hash`, `role`, `gender` (Enum: MALE, FEMALE), `height_cm`, `current_weight_kg`, `activity_level` (Enum: SEDENTARY, LIGHT, MODERATE, INTENSE), `goal` (Enum: LOSE_WEIGHT, MAINTAIN, GAIN_MUSCLE), `created_at` |
| **UserMetrics** | Historique d'évolution | `id`, `user_id`, `weight_kg`, `calorie_goal`, `recorded_date`                                                                                                                                                      |
| **FoodItem** | Aliment unitaire (Cache) | `id`, `barcode`, `name`, `kcal_100g`, `proteins_100g`, `carbs_100g`, `fats_100g`, `source` (OFF, Manual)                                                                                                           |
| **MealPreset** | Repas personnalisé (Preset) | `id`, `user_id`, `name` (ex: "Petit dej muscu")                                                                                                                                                                    |
| **MealIngredient** | Composant d'un Preset | `id`, `meal_preset_id`, `food_item_id`, `quantity_grams`                                                                                                                                                           |
| **DailyLog** | Journal d'une journée | `id`, `user_id`, `target_date`, `total_kcal`, `total_proteins`, etc.                                                                                                                                               |
| **LogEntry** | Entrée consommée | `id`, `daily_log_id`, `food_item_id` (null si repas), `meal_preset_id` (null si aliment), `quantity_grams`, `consumed_at`                                                                                          |

# RÈGLES MÉTIER ET LOGIQUE (BUSINESS RULES)

1. Gestion du Cache des Aliments (Fallback Logic) :
   Lorsqu'une recherche textuelle ou un scan de code-barre est effectué, l'API doit d'abord interroger la table `FoodItem`. Si l'aliment n'existe pas, l'API doit consommer l'API externe (Open Food Facts), formater le JSON de réponse, insérer le nouvel aliment dans `FoodItem`, puis retourner la réponse au client.

2. Enregistrement d'un LogEntry :
   Un utilisateur peut logger soit un `FoodItem` unitaire avec un grammage, soit un `MealPreset`. Si un `MealPreset` est loggé, l'API doit calculer dynamiquement la somme des macros de tous les `MealIngredient` associés pour ce log, et mettre à jour les totaux du `DailyLog`.

3. Données pour les Graphiques (Analytics) :
   Créer des endpoints spécifiques (ex: `/api/v1/analytics/macros?range=week`) qui agrègent les données des tables `DailyLog` et `UserMetrics` via des requêtes SQL optimisées (utiliser des requêtes `@Query` natives ou JPQL, éviter de ramener toutes les lignes en mémoire pour faire la somme en Java).

4. Explication de l'application:
   L'application est une API d'une application mobile de gestion des calories et macros d'une personne dans la journée et suivie. L'utilisateur peut se logger ou créer un compte. Lors de la création d'un compte il renseigne son poids et sa taille, son niveau d'activité physique et ce qu'il veut faire : "perte du poids" ou"prise de masse". Une fois connecté l'utilisateur arrive à l'accueil ou il voit ses calories et macros consommés par rapport à ce qu'il doit consommé dasn la journée. il peut ajouter un repas qu'il a mangé, scané un aliment qu'il a mangé et ajouté le grammage, ou saisir manuellement l'aliment. L'utilisateur a des repas préenregistrés (liste d'aliment avec grammage) et peut en ajouter/retirer, toujours avec la metode de scan de code barre ou de saisie manuelle. Il y a une page suivie où l'on voit l'evolution du poids, des calories et des macros sur le temps à travers des graphiques. Une page profile où il peut saisir son nouveau poids et changer son programme et ses objectifs.
5. Utilisation de l'API Open Food Facts et CIQUAL
    Utilisation de l'api open food facts pour les aliments scanné et ciqual pour les saisies manuelles lorsque l'aliment n'est pas dans le cache.

# RÈGLES DE CODAGE ET BONNES PRATIQUES

1. Architecture en Couches :
   Respecter strictement le pattern Controller -> Service -> Repository. Aucune logique métier dans les contrôleurs.

2. Utilisation des DTOs :
   Ne jamais exposer les entités JPA directement dans les contrôleurs. Toujours utiliser des classes DTO (Data Transfer Objects) en entrée (Request) et en sortie (Response).
3. Utilisation de mappers

4. Gestion des Exceptions :
   Utiliser un `@ControllerAdvice` global pour intercepter les exceptions (ex: `EntityNotFoundException`) et renvoyer des réponses HTTP standardisées (ProblemDetail ou JSON custom) avec les bons codes HTTP (400, 404, 500).

5. Code Propre et Lisible :
* Utiliser Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) pour réduire le code boilerplate.
* Privilégier l'injection de dépendances par constructeur (via `@RequiredArgsConstructor` de Lombok) plutôt que `@Autowired` sur les champs.
* Écrire des méthodes courtes avec des noms explicites en anglais.

# INSTRUCTIONS DE RÉPONSE POUR L'IA
* Génère toujours le code complet pour la classe demandée.
* Commente les parties complexes du code, notamment les requêtes JPA sur mesure et la logique de calcul des macros.
* Si une librairie externe est requise, fournis la balise XML exacte à ajouter dans le `pom.xml`.