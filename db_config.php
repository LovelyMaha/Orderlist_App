<?php
// Database Configuration for GoDaddy (or any host)
// Fill these in later when you create your MySQL Database!
$db_host = 'localhost'; 
$db_name = 'u483840491_order_tracker';
$db_user = 'u483840491_admin';
$db_pass = '@Prajith8250';

try {
    $pdo = new PDO("mysql:host=$db_host;dbname=$db_name;charset=utf8mb4", $db_user, $db_pass);
    // Set PDO error mode to exception
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch(PDOException $e) {
    die(json_encode(["status" => "error", "message" => "Database connection failed! Please fill out db_config.php"]));
}

/* 
=========================================================
SQL COMMAND TO CREATE THE TABLE:
Copy & Run this exactly in your GoDaddy phpMyAdmin SQL tab:
=========================================================

CREATE TABLE `order_images` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) NOT NULL,
  `label` varchar(100) NOT NULL,
  `image_path` text NOT NULL,
  `uploaded_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

*/
?>
