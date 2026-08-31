#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://localhost:8089}"
USER_ID="${USER_ID:-6a95972a51efe2707278ff6d}"
ROLE="${ROLE:-SELLER}"
AUTHORIZATION_HEADER="${AUTHORIZATION_HEADER:-Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJoZWxsbyIsInVzZXJJZCI6IjZhOTU5NzJhNTFlZmUyNzA3Mjc4ZmY2ZCIsInJvbGUiOiJDTElFTlQiLCJpYXQiOjE3ODgxODg0ODQsImV4cCI6MTc4ODI3NDg4NH0.r-XUDM62xE0rGLpCvGFeGV0SeMLqlXzV3W3zx7kXn2q4NImM54MRJBmHrcmsdk5d}"



for i in $(seq 1 10000); do
  name="Test Product ${i}"
  description="Auto-generated product ${i} for pagination testing"
  price=$(awk -v i="$i" 'BEGIN {printf "%.2f", 20 + (i % 60) + ((i % 7) * 0.5)}')
  quantity=$(( (i % 10) + 1 ))
  category=$(( (i % 3) + 1 ))
  case "$category" in
    1) category="Streetwear" ;;
    2) category="Outerwear" ;;
    *) category="Accessories" ;;
  esac

  curl -k -sS -X POST "$BASE_URL/api/products" \
    -H "X-User-Id: $USER_ID" \
    -H "X-Role: $ROLE" \
    -H "Authorization: $AUTHORIZATION_HEADER" \
    -F "name=${name}" \
    -F "description=${description}" \
    -F "price=${price}" \
    -F "quantity=${quantity}" \
    -F "category=${category}" \
    >> ~/Desktop/market02/create-product-response.json

  # echo "Created product $i"
done

echo "Done: 10000 products created."