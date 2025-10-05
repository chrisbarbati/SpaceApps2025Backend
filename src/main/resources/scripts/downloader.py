import earthaccess
import datetime
import os
import time
import xarray as xr
import re
import pandas as pd
from dotenv import load_dotenv
import argparse
import gc

load_dotenv()


def create_shortened_file(filepath):
    """
    Strips a TEMPO NetCDF file down to essential variables. It replaces the
    original file with the new, smaller version.

    It also checks if the file has already been processed to avoid errors.

    Args:
        filepath (str): The full path to the original TEMPO .nc file.
    """
    # --- Check if the file has already been shortened to prevent errors ---
    try:
        with xr.open_dataset(filepath) as ds:
            history = ds.attrs.get('history', '')
            if "File stripped by user script" in history:
                print(
                    f"-> File '{os.path.basename(filepath)}' has already been shortened. Skipping.")
                return
    except Exception:
        # If it fails to open (e.g., it's mid-download), we'll let the main process continue
        pass

    print("\n" + "=" * 80)
    print(f"Processing and shortening: {os.path.basename(filepath)}")
    print("-" * 80)

    # --- 1. Define a temporary output filename ---
    # We will rename it to the original name at the end
    base, ext = os.path.splitext(filepath)
    temp_output_filepath = f"{base}_temp_shortened{ext}"

    # --- 2. Define the variables we want to keep ---
    vars_to_keep = {
        'support_data': [
            'vertical_column_total',
            'vertical_column_total_uncertainty'
        ],
        'product': [
            'main_data_quality_flag'
        ]
    }

    try:
        # --- 3. Open the original file to get root info ---
        with xr.open_dataset(filepath) as ds_root:
            ds_shortened = xr.Dataset(coords=ds_root.coords)
            ds_shortened.attrs = ds_root.attrs

        # --- 4. Open each required group and add the desired variables ---
        for group, var_list in vars_to_keep.items():
            with xr.open_dataset(filepath, group=group) as ds_group:
                for var_name in var_list:
                    if var_name in ds_group:
                        ds_shortened[var_name] = ds_group[var_name].load()
                        print(f"  - Keeping variable: {group}/{var_name}")
                    else:
                        print(
                            f"  - Warning: Variable '{var_name}' not found in group '{group}'.")

        # --- 5. Add a note to the history attribute ---
        timestamp = datetime.datetime.now(
            datetime.timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
        history_note = f"\n{timestamp}: File stripped by user script. Kept only essential variables."
        ds_shortened.attrs['history'] = ds_shortened.attrs.get(
            'history', '') + history_note

        # --- 6. Save the new, smaller file to a temporary name ---
        ds_shortened.to_netcdf(temp_output_filepath, engine='h5netcdf')

        print(f"\nSuccessfully created temporary shortened file.")
        original_size = os.path.getsize(filepath) / (1024 * 1024)
        new_size = os.path.getsize(temp_output_filepath) / (1024 * 1024)
        print(f"Original size: {original_size:.2f} MB")
        print(f"New size:      {new_size:.2f} MB")
        print(
            f"Reduced size by {original_size - new_size:.2f} MB ({100 * (1 - new_size/original_size):.1f}%)")

        ds_shortened.close()
        gc.collect

        time.sleep(2)

        # --- 7. Delete the original large file ---
        print(f"\nDeleting original file: {os.path.basename(filepath)}")
        os.remove(filepath)
        print("Original file deleted.")

        # --- 8. Rename the temporary file to the original filename ---
        print(f"Renaming temporary file to '{os.path.basename(filepath)}'")
        os.rename(temp_output_filepath, filepath)
        print("File successfully replaced with its shortened version.")
        print("=" * 80)

    except Exception as e:
        print(f"\nAn error occurred while shortening the file: {e}")
        # Clean up the temp file if it was created
        if os.path.exists(temp_output_filepath):
            os.remove(temp_output_filepath)
            print("Removed temporary file.")


# --- Step 1: Log in to Earthdata ---
# Add argument parser before login
parser = argparse.ArgumentParser(
    description='Download and process TEMPO NO2 data')
parser.add_argument('-granules', '--granules', type=int, default=1,
                    help='Number of latest granules to download (default: 1)')
args = parser.parse_args()

try:
    auth = earthaccess.login()
    if not auth.authenticated:
        print("Login failed. Please check your credentials.")
        exit()
    print("Successfully logged in to Earthdata.")
except Exception as e:
    print(f"An error occurred during login: {e}")
    exit()

# --- Step 2: Define Time Frame for Latest Data ---
end_date = datetime.datetime.now(datetime.timezone.utc)
start_date = end_date - datetime.timedelta(days=7)
temporal_range = (start_date.strftime("%Y-%m-%dT%H:%M:%SZ"),
                  end_date.strftime("%Y-%m-%dT%H:%M:%SZ"))

print(
    f"Searching for latest TEMPO NO2 data from {temporal_range[0]} to {temporal_range[1]}")

# --- Step 3: Search for the Latest Data ---
try:
    results = earthaccess.search_data(
        short_name='TEMPO_NO2_L3',
        cloud_hosted=True,
        temporal=temporal_range,
    )
except Exception as e:
    print(f"An error occurred during the search: {e}")
    exit()

if not results:
    print("No data found for the specified time range.")
    exit()

print(
    f"Found {len(results)} data granules. Processing the latest {args.granules}.")

# --- Step 4: Download the Latest Data ---
download_dir = "./tempo_data"
if not os.path.exists(download_dir):
    os.makedirs(download_dir)

try:
    print("Starting download...")
    start_time = time.time()
    # Download the specified number of latest granules
    num_to_download = 5 #min(args.granules, len(results))
    files = earthaccess.download(
        results[-num_to_download:], local_path=download_dir)
    end_time = time.time()
    download_duration = end_time - start_time

    print(
        f"Successfully downloaded {num_to_download} file(s) to the '{download_dir}' directory.")
    print(f"Download completed in {download_duration:.2f} seconds")

    # --- Step 5: Process the downloaded file(s) ---
    files_to_process = files if isinstance(files, list) else [files]

    for downloaded_file in files_to_process:
        if downloaded_file and os.path.exists(downloaded_file):
            create_shortened_file(downloaded_file)
        else:
            print(
                f"Could not find the downloaded file to process: {downloaded_file}")


except Exception as e:
    print(f"An error occurred during download or processing: {e}")
    exit()
