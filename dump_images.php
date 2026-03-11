<?php
require_once 'db_config.php';
try {
    $stmt = $pdo->query("SELECT * FROM order_images");
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode(["status" => "success", "count" => count($rows), "data" => $rows]);
} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => "Database Error: " . $e->getMessage()]);
}
?>
