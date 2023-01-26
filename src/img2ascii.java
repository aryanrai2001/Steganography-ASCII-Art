import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Objects;

public class img2ascii
{
    private final String charMap = "$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!l;:,\"^`'. ";
    public void writeASCII(String imagePath, String asciiPath, String message, boolean inverted) throws IOException
    {
        File asciiFile = new File(asciiPath);
        File imageFile = new File(imagePath);
        BufferedImage image = null;
        try
        {
            image = ImageIO.read(Objects.requireNonNull(imageFile));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        assert image != null;
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();
        int[] pixels = image.getRGB(0, 0, imgWidth, imgHeight, null, 0, imgWidth);
        short messageLength = (short) message.length();
        StringBuilder messageBinary = new StringBuilder();
        for (int i = 0; i < 16; i++)
            messageBinary.append((messageLength >> (15 - i)) & 1);
        for (int i = 0; i < messageLength; i++)
        {
            byte ch = (byte) message.charAt(i);
            for (int j = 0; j < 8; j++)
                messageBinary.append((ch >> (7 - j)) & 1);
        }
        message = messageBinary.toString();
        int messageIndex = 0;
        int gridRes = 8;
        int charH = imgHeight / gridRes;
        int charW = imgWidth / gridRes;
        FileWriter fileWriter = new FileWriter(asciiFile);
        fileWriter.write("");
        fileWriter.flush();
        for (int y = 0; y < charH; y++)
        {
            for (int x = 0; x < charW; x++)
            {
                int yStart = y * gridRes;
                int yEnd = yStart + gridRes;
                int xStart = x * gridRes;
                int xEnd = xStart + gridRes;
                int intensity = 0;
                for (int yPos = yStart; yPos < yEnd; yPos++)
                {
                    for (int xPos = xStart; xPos < xEnd; xPos++)
                    {
                        int color = pixels[xPos + yPos * imgWidth];
                        int pixIntensity =  ((color >> 16) & 255); //Red Value
                        // int pixIntensity = ((color & 255) + ((color >> 8) & 255) + ((color >> 16) & 255)) / 3; //Average Value
                        intensity += pixIntensity;
                    }
                }
                intensity /= (gridRes * gridRes);
                float mapValue = (intensity/256.0f)*charMap.length();
                int offsetBias = Math.round(mapValue * 10) % 10;
                int mapIndex = (int)mapValue;
                if (messageIndex < message.length()) mapIndex += (message.charAt(messageIndex++) == ('0' + (mapIndex % 2)))?0:((offsetBias > 4)?1:-1);
                if (mapIndex < 0) mapIndex = 1;
                else if (mapIndex >= charMap.length()) mapIndex = charMap.length() - 2;
                char currChar = charMap.charAt(inverted?charMap.length()-mapIndex-1:mapIndex);
                fileWriter.append(String.valueOf(currChar)).append(" ");
                fileWriter.flush();
            }
            fileWriter.append('\n');
            fileWriter.flush();
        }
        fileWriter.close();
    }

    private String readASCII(String asciiPath) throws IOException
    {
        File asciiFile = new File(asciiPath);
        if (!asciiFile.exists())
        {
            System.err.println("Error: File Not Found (" + asciiFile.getAbsolutePath() + ")");
            System.exit(1);
        }
        FileReader inputFile = new FileReader(asciiFile);
        StringBuilder message = new StringBuilder();
        String inputText = Files.readString(asciiFile.toPath());
        if (inputText == null || inputText.length() == 0)
        {
            System.err.println("Error: File Is Empty (" + asciiFile.getAbsolutePath() + ")");
            System.exit(1);
        }
        for (int i = 0; i < 16; i++)
        {
            char ch = inputText.charAt(i*2);
            message.append((char) ((charMap.indexOf(ch) % 2) + '0'));
        }
        int msgLength = (Integer.parseInt(message.toString(), 2) + 2) * 8;
        message = new StringBuilder();
        StringBuilder letter = new StringBuilder();
        int offset = 0;
        for (int i = 16; i < msgLength; i++)
        {
            char ch = inputText.charAt(i*2 + offset);
            if (ch == '\n')
            {
                offset++;
                i--;
                continue;
            }
            letter.append((char) ((charMap.indexOf(ch) % 2) + '0'));
            if (letter.length() == 8)
            {
                message.append((char) Integer.parseInt(letter.toString(), 2));
                letter = new StringBuilder();
            }
        }
        inputFile.close();
        return message.toString();
    }

    public static void main(String[] args)
    {
        String imagePath = null, asciiPath = null, absImagePath = null, absASCIIPath = null, message = null;
        boolean inverted = false;

        if (args.length == 1)
        {
            asciiPath = args[0];
        }
        else if (args.length == 3)
        {
            inverted = Integer.parseInt(args[0]) == 1;
            asciiPath = "output.txt";
            imagePath = args[1];
            message = args[2];
        }
        else
        {
            System.out.println("Usage -");
            System.out.println("Encode: img2ascii [Inverted(0/1)] [ImageFilePath(path/file.png)] [MESSAGE]");
            System.out.println("        Example - img2ascii 0 \"./sampleImageFile.png\" \"Hello, World!\"");
            System.out.println("Decode: img2ascii [TextFilePath(path/file.txt)]");
            System.out.println("        Example - img2ascii \"./outputTextFile.txt\"");
            System.exit(0);
        }

        if (imagePath != null) absImagePath = FileSystems.getDefault().getPath(imagePath).normalize().toAbsolutePath().toString();
        if (asciiPath != null) absASCIIPath = FileSystems.getDefault().getPath(asciiPath).normalize().toAbsolutePath().toString();

        img2ascii obj = new img2ascii();

        try {
            if (absImagePath != null)
            {
                obj.writeASCII(absImagePath, absASCIIPath, message, inverted);
                System.out.println("Success! : Output saved to (" + absASCIIPath + ")");
            }
            else
            {
                System.out.println(obj.readASCII(absASCIIPath));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}