<?php
// get_images.php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

require_once 'db_config.php'; // Include SQL database connection

$phone = isset($_GET['phone']) ? trim($_GET['phone']) : '';

if (empty($phone)) {
    echo json_encode(["status" => "error", "images" => []]);
    exit();
}

try {
    // Fetch images from MySQL database for this phone number, including ID
    $stmt = $pdo->prepare("SELECT id, label, image_path AS image FROM order_images WHERE phone = :phone ORDER BY uploaded_at DESC");
    $stmt->execute([':phone' => $phone]);
    
    $images = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "status" => "success",
        "images" => $images
    ]);

} catch (PDOException $e) {
    echo json_encode(["status" => "error", "images" => [], "message" => "Database Error: " . $e->getMessage()]);
}
?>
