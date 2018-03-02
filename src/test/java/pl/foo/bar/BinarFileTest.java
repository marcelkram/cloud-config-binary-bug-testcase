package pl.foo.bar;

import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test case shows that the Edgware.SR2 release Cloud Config server serves binary files in an damaged form.
 * All files requests are always handled by the ResourceController.retrieve method.
 *
 * @author Marceli Kramarczuk
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigServerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class BinarFileTest {

    //this works fine
    @Test
    public void shouldCorrectlyRetrieveTextFile() throws IOException {
        //given
        final String expectedFileContent = Resources.toString(Resources.getResource("files/foo/bar/text.txt"), StandardCharsets.UTF_8);

        //when
        HttpURLConnection con = (HttpURLConnection) new URL("http://localhost:8888/foo/bar/master/text.txt").openConnection();
        final String result = getAsUtf8String(con);

        //then
        assertThat(result).isEqualTo(expectedFileContent);
    }

    //now the binary file
    //this test will fail
    @Test
    public void shouldCorrectlyRetrieveBinaryFile() throws IOException {
        //given
        final byte[] expectedFileContent = Resources.toByteArray(Resources.getResource("files/foo/bar/rm.jpg"));

        //when
        HttpURLConnection con = (HttpURLConnection) new URL("http://localhost:8888/foo/bar/master/rm.jpg").openConnection();
        final byte[] result = getAsByteArray(con);

        //then
        assertThat(result).isEqualTo(expectedFileContent);
    }

    //Setting headers as suggested does not help
    @Test
    public void shouldCorrectlyRetrieveBinaryFileWithHeaders() throws IOException {
        //given
        final byte[] expectedFileContent = Resources.toByteArray(Resources.getResource("files/foo/bar/rm.jpg"));

        //when
        HttpURLConnection con = (HttpURLConnection) new URL("http://localhost:8888/foo/bar/master/rm.jpg").openConnection();
        con.setRequestProperty("Accept", MediaType.APPLICATION_OCTET_STREAM_VALUE);

        final byte[] result = getAsByteArray(con);

        //then
        assertThat(result).isEqualTo(expectedFileContent);
    }

    //this will pass, we read the source file in the same invalid (in the context of reading binary files) way as the server
    @Test
    public void fileDamagingProcessShowcase() throws IOException {
        //given
        final byte[] expectedFileContent = Resources.toString(Resources.getResource("files/foo/bar/rm.jpg"), StandardCharsets.UTF_8).getBytes();

        //when

        //we have to set the resolvePlaceholders parameter to false, otherwise the file gets damaged even further
        HttpURLConnection con = (HttpURLConnection) new URL("http://localhost:8888/foo/bar/master/rm.jpg?resolvePlaceholders=false").openConnection();
        con.setRequestProperty("Accept", "application/octet-stream");
        final byte[] result = getAsByteArray(con);

        //then
        assertThat(result).isEqualTo(expectedFileContent);
    }

    private String getAsUtf8String(HttpURLConnection con) throws IOException {
        try {
            return IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8);
        } finally {
            con.disconnect();
        }
    }

    private byte[] getAsByteArray(HttpURLConnection con) throws IOException {
        try {
            return IOUtils.toByteArray(con.getInputStream());
        } finally {
            con.disconnect();
        }
    }
}