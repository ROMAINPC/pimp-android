#pragma version (1)
#pragma rs java_package_name (fr.ubordeaux.pimp)
#pragma rs_fp_relaxed
#include "utils.rs"

rs_allocation pIn;
rs_allocation pOut;
uint32_t width, height;
uint32_t kWidth, kHeight;
static uint32_t kCenterX, kCenterY;
float kdiv; //For normalize pixel with total value of kernel
const float* kernel;
const float* sobelY;
const float* sobelX;
bool normal;


uchar4 RS_KERNEL conv2d(uchar4 in, uint32_t x, uint32_t y)
{
    if (!(x >= (kCenterX) && x < (width - kCenterX) && y >= (kCenterY) && y < (height - kCenterY)))
        return rsPackColorTo8888(0.0f,0.0f,0.0f,1.0f);
    uint32_t kx, ky;
    float4 temp = 0;

    uint32_t kIndex = 0;
    for(ky = y - kCenterY; ky <= y + kCenterY ;ky++)
    {
        for(kx = x - kCenterX; kx <= x + kCenterX ;kx++)
        {

            temp += rsUnpackColor8888( rsGetElementAt_uchar4(pIn, kx, ky)) * kernel[kIndex];
            kIndex++;

        }
    }

    if (normal) temp /= kdiv; //Normalize
    temp = fabs(temp);

    temp.a = 1.00f;
    return rsPackColorTo8888(temp);
}



uchar4 RS_KERNEL conv2dSobel(uchar4 in, uint32_t x, uint32_t y) //Image must be gray!!
{
    if (!(x >= (kCenterX) && x < (width - kCenterX) && y >= (kCenterY) && y < (height - kCenterY)))
        return rsPackColorTo8888(0.0f,0.0f,0.0f,1.0f);
    uint32_t kx, ky;
    float sum = 0;
    float tempX = 0;
    float tempY = 0;
    float4 pixelf;
    uint32_t kIndex = 0;
    for(ky = y - kCenterY; ky <= y + kCenterY ;ky++)
    {
        for(kx = x - kCenterX; kx <= x + kCenterX ;kx++)
        {

            pixelf = rsUnpackColor8888( rsGetElementAt_uchar4(pOut, kx, ky)); //Get only one channel cause greyscale image
            tempX += pixelf.r * sobelX[kIndex];
            tempY += pixelf.r * sobelY[kIndex];
            kIndex++;



        }
    }

    sum = fabs(tempX) + fabs(tempY);

    return rsPackColorTo8888(sum,sum,sum, 1.00f);
}




void setup(){
    kCenterX = kWidth/2;
    kCenterY = kHeight/2;
}


void sobelOperator(rs_allocation inputImage, rs_allocation outputImage){
    rsForEach(grey, inputImage, outputImage); // Turn to gray
    setup(); //Init kCenters
    rsForEach(conv2dSobel,outputImage,inputImage);
}



