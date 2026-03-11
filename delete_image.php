<?php
// delete_image.php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");

require_once 'db_config.php';

$id = isset($_GET['id']) ? intval($_GET['id']) : 0;

if ($id <= 0) {
    echo json_encode(["status" => "error", "message" => "Invalid ID"]);
    exit();
}

try {
    // Fetch image_path to optionally delete file from disk to save space
    $stmt = $pdo->prepare("SELECT image_path FROM order_images WHERE id = :id");
    $stmt->execute([':id' => $id]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($row) {
        $path = $row['image_path'];
        $filename = basename($path);
        $localFile = __DIR__ . '/order_images/' . $filename;
        if (file_exists($localFile)) {
            unlink($localFile);
        }
    }

    $stmt = $pdo->prepare("DELETE FROM order_images WHERE id = :id");
    $stmt->execute([':id' => $id]);

    echo json_encode(["status" => "success", "message" => "Image deleted"]);

} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => "Database Error: " . $e->getMessage()]);
}
?>
