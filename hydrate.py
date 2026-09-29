import csv
import requests
import os

# Your API Gateway port (adjust if your gateway runs on a different port)
GATEWAY_URL = "http://localhost:8081/api/products" 
DATASET_DIR = "./dataset"

# Create a hardcoded map of your categories so they get proper, immutable IDs
CATEGORY_MAP = {
    "amazon_laptop.csv": { "id": "cat-550e8400-0000", "name": "Laptops", "slug": "laptops" },
    "amazon_audio_video.csv": { "id": "cat-550e8400-0001", "name": "Audio & Video", "slug": "audio-video" },
    "amazon_camra.csv": { "id": "cat-550e8400-0002", "name": "Cameras", "slug": "cameras" },
    "amazon_car_accessories.csv": { "id": "cat-550e8400-0003", "name": "Car Accessories", "slug": "car-accessories" },
    "amazon_men.csv": { "id": "cat-550e8400-0004", "name": "Men's Fashion", "slug": "mens-fashion" },
    "amazon_men_shoe.csv": { "id": "cat-550e8400-0005", "name": "Men's Shoes", "slug": "mens-shoes" },
    "amazon_mobile.csv": { "id": "cat-550e8400-0006", "name": "Mobiles", "slug": "mobiles" },
    "amazon_movies.csv": { "id": "cat-550e8400-0007", "name": "Movies", "slug": "movies" },
    "amazon_toys_1.csv": { "id": "cat-550e8400-0008", "name": "Toys", "slug": "toys" }
}

def clean_product_name(raw_name):
    # Split by common delimiters that often separate the real name from the specs
    delimiters = [',', ' -', ' |', ' (']
    
    shortest_name = raw_name
    
    for delimiter in delimiters:
        if delimiter in raw_name:
            candidate = raw_name.split(delimiter)[0].strip()
            # We don't want a name that is too short (e.g. just "HP")
            if len(candidate) > 3 and len(candidate) < len(shortest_name):
                shortest_name = candidate
                
    return shortest_name

def hydrate_category(filename, limit=500):
    filepath = os.path.join(DATASET_DIR, filename)
    category_data = CATEGORY_MAP.get(filename)
    
    if not category_data:
        print(f"Error: No category mapping found for {filename}")
        return

    print(f"--- Starting Hydration for {category_data['name']} ---")
    
    if not os.path.exists(filepath):
        print(f"Error: File not found at {filepath}")
        return

    with open(filepath, mode='r', encoding='latin-1') as file:
        reader = csv.DictReader(file)
        count = 0
        
        for row in reader:
            if count >= limit:
                break
            
            # Extract and clean data safely
            raw_desc = row.get("Product Description", "Unknown Product")
            clean_name = clean_product_name(raw_desc)
            price_str = row.get("Price(Dollar)", "0.00")
            
            # Clean price string (sometimes CSVs have "None" or "$")
            if price_str == "None" or not price_str.strip():
                price_str = "0.00"
            else:
                # Remove currency symbols and commas if present
                price_str = price_str.replace('$', '').replace(',', '')
                
            # Construct the JSON Payload matching your Data Contract
            payload = {
                "name": clean_name,
                "description": raw_desc,
                "price": float(price_str),
                "quantity": 100, # Default quantity
                "categoryId": category_data["id"],
                "attributes": {
                    "reviews": row.get("Number of reviews", "0"),
                    "real_price": row.get("Real price(Dollar)", ""),
                    "free_days": row.get("Free days", ""),
                    "shipment": row.get("Shipment", ""),
                    "delivery_date": row.get("Delivery Date", ""),
                    "category_name": category_data["name"],
                    "category_slug": category_data["slug"]
                }
            }
            
            # Fire the request to the API Gateway
            try:
                response = requests.post(GATEWAY_URL, json=payload)
                if response.status_code in [200, 201]:
                    print(f"[{count+1}/{limit}] Indexed: {payload['name'][:40]}")
                    count += 1
                else:
                    print(f"Failed: {response.status_code} - {response.text}")
            except Exception as e:
                print(f"Network error: {e}")
                break

# Run the script for all categories in the dataset folder
if __name__ == "__main__":
    print("Starting full dataset hydration...")
    
    # Iterate through all files in the CATEGORY_MAP
    # for filename in CATEGORY_MAP.keys():
    #     hydrate_category(filename, limit=500)
    
    # Just run one for testing
    hydrate_category("amazon_laptop.csv", limit=5)
        
    print("Hydration Complete!")
