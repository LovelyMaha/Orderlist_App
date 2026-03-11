<?php
// upload_image.php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once 'db_config.php'; // Include SQL database connection

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["status" => "error", "message" => "Only POST method allowed"]);
    exit();
}

$phone = isset($_POST['orderphonenumber']) ? trim($_POST['orderphonenumber']) : '';
$label = isset($_POST['label'])            ? trim($_POST['label'])            : '';

if (empty($phone) || empty($label)) {
    echo json_encode(["status" => "error", "message" => "Missing orderphonenumber or label"]);
    exit();
}

if (!isset($_FILES['image']) || $_FILES['image']['error'] !== UPLOAD_ERR_OK) {
    echo json_encode(["status" => "error", "message" => "No image uploaded or upload error"]);
    exit();
}

$safePhone = preg_replace('/[^0-9+]/', '', $phone);
$safeLabel = strtolower(preg_replace('/[^a-zA-Z0-9]/', '', $label)); 

$uploadDir = __DIR__ . '/order_images/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0777, true);
}

// Ensure unique filename
$baseFilename = $safePhone . '_' . $safeLabel . '.jpeg';
$filename     = $baseFilename;
$counter      = 1;
while (file_exists($uploadDir . $filename)) {
    $filename = $safePhone . '_' . $safeLabel . '_' . $counter . '.jpeg';
    $counter++;
}

// Compress & convert to JPEG (Kept this so large 10MB mobile images don't crash your server over time!)
$tmpPath = $_FILES['image']['tmp_name'];
$imgInfo = @getimagesize($tmpPath);
if (!$imgInfo) {
    echo json_encode(["status" => "error", "message" => "Invalid image uploaded."]);
    exit();
}

$mimeType = $imgInfo['mime'];
$srcImage = null;

switch ($mimeType) {
    case 'image/jpeg': case 'image/jpg': $srcImage = @imagecreatefromjpeg($tmpPath); break;
    case 'image/png': $srcImage = @imagecreatefrompng($tmpPath); break;
    case 'image/webp': $srcImage = @imagecreatefromwebp($tmpPath); break;
    default:
        echo json_encode(["status" => "error", "message" => "Unsupported image type."]);
        exit();
}

if (!$srcImage) {
    echo json_encode(["status" => "error", "message" => "Failed to process image."]);
    exit();
}

// Resize if too large
$origW = imagesx($srcImage);
$origH = imagesy($srcImage);
$maxW  = 1200;
if ($origW > $maxW) {
    $newH     = (int)(($origH / $origW) * $maxW);
    $resized  = imagecreatetruecolor($maxW, $newH);
    imagecopyresampled($resized, $srcImage, 0, 0, 0, 0, $maxW, $newH, $origW, $origH);
    imagedestroy($srcImage);
    $srcImage = $resized;
}

$savePath = $uploadDir . $filename;
$saved    = imagejpeg($srcImage, $savePath, 80);
imagedestroy($srcImage);

if (!$saved) {
    echo json_encode(["status" => "error", "message" => "Failed to save image file to server."]);
    exit();
}

// Build dynamic public URL (Works automatically on GoDaddy or any domain)
$protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
$host = $_SERVER['HTTP_HOST'];
$scriptPath = dirname($_SERVER['PHP_SELF']);
$scriptPath = $scriptPath === '/' || $scriptPath === '\\' ? '' : $scriptPath;
$imageUrl  = $protocol . "://" . $host . $scriptPath . "/order_images/" . $filename;

// ── SAVE TO MYSQL DATABASE (Replaces old CSV logic) ──
try {
    $stmt = $pdo->prepare("INSERT INTO order_images (phone, label, image_path) VALUES (:phone, :label, :image_path)");
    $stmt->execute([
        ':phone' => $phone,
        ':label' => $label,
        ':image_path' => $imageUrl
    ]);

    echo json_encode([
        "status"  => "success",
        "image"   => $imageUrl,
        "label"   => $label,
        "message" => "Image uploaded and saved to MySQL successfully"
    ]);

} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => "Database Error: " . $e->getMessage()]);
}
?>
