#include <iostream>
#include <filesystem>
namespace fs = std::filesystem;

bool download_model(const std::string& model_path) {
    // If model has not been downloaded, download it from
    // curl -L -O https://huggingface.co/runwayml/stable-diffusion-v1-5/resolve/main/v1-5-pruned-emaonly.safetensors
    std::string command = "curl -L -o " + model_path + " https://huggingface.co/runwayml/stable-diffusion-v1-5/resolve/main/v1-5-pruned-emaonly.safetensors";
    int result = system(command.c_str());
    if (result == 0) {
        std::cout << "Model downloaded successfully to: " << model_path << std::endl;
        return true;
    } else {
        std::cerr << "Failed to download model." << std::endl;
        return false;
    }
}

bool verify_download(const std::string& file_path, const std::string& expected_sha256) {
    // Verifies the SHA256 checksum of the downloaded file
    std::string command = "shasum -a 256 " + file_path + " | awk '{print $1}'";
    FILE* pipe = popen(command.c_str(), "r");
    if (!pipe) {
        std::cerr << "Failed to open pipe for checksum verification." << std::endl;
        return false;
    }
    char buffer[65];
    fgets(buffer, sizeof(buffer), pipe);
    pclose(pipe);
    std::string actual_sha256(buffer);
    actual_sha256.erase(actual_sha256.find_last_not_of(" \n\r\t")+1); // Trim whitespace

    if (actual_sha256 == expected_sha256.substr(7)) { // Remove 'sha256:' prefix
        std::cout << "Checksum verification passed." << std::endl;
        return true;
    } else {
        std::cerr << "Checksum verification failed!" << std::endl;
        return false;
    }
}

bool download_stable_diffusion(){
    // Downloads the stable diffusion executable, if not already present
    struct platformInfo {
        std::string url;
        std::string sha256;
    };
    
    platformInfo macOS;
    macOS.url = "https://github.com/leejet/stable-diffusion.cpp/releases/download/master-343-dd75fc0/sd-master--bin-Darwin-macOS-15.7.1-arm64.zip";
    macOS.sha256 = "sha256:49bb1c0273efb6a36a26926ece674daffe49cd4a51c9e8935b5c9e8eb68b7ea2";

    platformInfo linux;
    linux.url = "https://github.com/leejet/stable-diffusion.cpp/releases/download/master-343-dd75fc0/sd-master--bin-Linux-Ubuntu-24.04-x86_64.zip";
    linux.sha256 = "sha256:152df5843e2ea265a627024de37a985cf75b5554554e2ad5d0ff06aad76ba4d8";

    platformInfo windows;
    windows.url = "https://github.com/leejet/stable-diffusion.cpp/releases/download/master-343-dd75fc0/sd-master-dd75fc0-bin-win-avx-x64.zip";
    windows.sha256 = "sha256:17f6d4f4e1cdaf92f90ff09479e0460246193d015f2b29f8f7553affed426c78";

    // Determine platform and download accordingly
    #ifdef __APPLE__
        platformInfo current_platform = macOS;
    #elif __linux__
        platformInfo current_platform = linux;
    #elif _WIN32
        platformInfo current_platform = windows;
    #else
        std::cerr << "Unsupported platform!" << std::endl;
        return false;
    #endif
    std::cout << "Downloading stable diffusion from: " << current_platform.url << std::endl;

    // Create supplementary directory if it doesn't exist
    fs::create_directories("./supplementary");

    // Download the file using curl
    std::string command = "curl -L -o ./supplementary/stable_diffusion.zip " + current_platform.url;
    int result = system(command.c_str());
    if (result == 0) {
        std::cout << "Stable diffusion downloaded successfully." << std::endl;
        if (!verify_download("./supplementary/stable_diffusion.zip", current_platform.sha256)) {
            std::cerr << "Downloaded file verification failed. Exiting." << std::endl;
            return false;
        }
        // Unzip the file
        std::string unzip_command;
        #ifdef _WIN32
            unzip_command = "tar -xf ./supplementary/stable_diffusion.zip -C .\\supplementary\\";
        #else
            unzip_command = "unzip ./supplementary/stable_diffusion.zip -d ./supplementary/";
        #endif
        result = system(unzip_command.c_str());
        if (result == 0) {
            std::cout << "Stable diffusion unzipped successfully." << std::endl;
            return true;
        } else {
            std::cerr << "Failed to unzip stable diffusion." << std::endl;
            return false;
        }
    } else {
        std::cerr << "Failed to download stable diffusion." << std::endl;
        return false;
    }
}

int main(){
    // Check whether model exists; if not download it
    std::cout << "Working path is: ";
    std::cout << fs::current_path() << std::endl;

    std::string model_path = "./models/v1-5-pruned-emaonly.safetensors";

    bool model_exists = fs::exists(model_path);
    if (model_exists) {
        std::cout << "Model found at: " << model_path << std::endl;
    } else {
        std::cout << "Model not found. Downloading model..." << std::endl;
        // Create models directory if it doesn't exist
        fs::create_directories("./models");

    
        if (!download_model(model_path)) {
            std::cerr << "Model download failed. Exiting." << std::endl;
            return 1;
        }
    }

    // Check whether stable diffusion executable exists; if not download it
    bool stable_diffusion_exists = fs::exists("./supplementary/sd");
    if (!stable_diffusion_exists) {
        std::cout << "Stable Diffusion executable not found. Downloading..." << std::endl;
        if (!download_stable_diffusion()) {
            std::cerr << "Stable Diffusion download failed. Exiting." << std::endl;
            return 1;
        }
    } else {
        std::cout << "Stable Diffusion executable found." << std::endl;
    }

    // Ask for a prompt
    std::string prompt;
    std::cout << "Enter your prompt: ";
    std::getline(std::cin, prompt);

    // Ask for output filename
    std::string output_filename;
    std::cout << "Enter output filename (with .png extension): ";
    std::getline(std::cin, output_filename);

    // load path
    std::string command = "install_name_tool -add_rpath @loader_path ./supplementary/sd";
    system(command.c_str());

    // Call stable diffusion with the prompt
    std::string sd_command = "./supplementary/sd -m " + model_path + " -p \"" + prompt + "\"" + " -o " + output_filename;
    std::cout << "Running Stable Diffusion with command: " << sd_command << std::endl;
    int sd_result = system(sd_command.c_str());
    if (sd_result != 0) {
        std::cerr << "Stable Diffusion execution failed." << std::endl;
        return 1;
    }

    return 0;
}