package test_locally.api.methods.impl;

import com.slack.api.RequestConfigurator;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.impl.FilesUploadV2Helper;
import com.slack.api.methods.request.files.FilesCompleteUploadExternalRequest;
import com.slack.api.methods.request.files.FilesUploadV2Request;
import com.slack.api.methods.response.files.FilesCompleteUploadExternalResponse;
import com.slack.api.methods.response.files.FilesUploadV2Response;
import com.slack.api.model.File;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.util.http.SlackHttpClient;
import okhttp3.OkHttpClient;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class FilesUploadV2HelperTest {

    @Test
    public void completeUploads_propagatesBlocksAndBlocksAsString() throws Exception {
        MethodsClient client = Mockito.mock(MethodsClient.class);
        when(client.getSlackHttpClient()).thenReturn(new SlackHttpClient(new OkHttpClient()));

        List<LayoutBlock> blocks = new ArrayList<LayoutBlock>();
        blocks.add(SectionBlock.builder().build());

        FilesUploadV2Request v2Request = FilesUploadV2Request.builder()
                .token("xoxb-test-token")
                .channel("C123")
                .channels(Arrays.asList("C123", "C234"))
                .initialComment("hello")
                .threadTs("123.456")
                .blocks(blocks)
                .blocksAsString("[{\"type\":\"section\"}]")
                .build();

        List<FilesCompleteUploadExternalRequest.FileDetails> files = Arrays.asList(
                FilesCompleteUploadExternalRequest.FileDetails.builder()
                        .id("F111")
                        .title("sample.txt")
                        .build()
        );

        AtomicReference<FilesCompleteUploadExternalRequest> capturedRequest = new AtomicReference<FilesCompleteUploadExternalRequest>();
        FilesCompleteUploadExternalResponse completionResponse = new FilesCompleteUploadExternalResponse();
        completionResponse.setOk(true);
        completionResponse.setFiles(Arrays.asList(File.builder().id("F111").title("sample.txt").build()));

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RequestConfigurator<FilesCompleteUploadExternalRequest.FilesCompleteUploadExternalRequestBuilder> configurator =
                    (RequestConfigurator<FilesCompleteUploadExternalRequest.FilesCompleteUploadExternalRequestBuilder>) invocation.getArguments()[0];
            FilesCompleteUploadExternalRequest builtRequest = configurator.configure(FilesCompleteUploadExternalRequest.builder()).build();
            capturedRequest.set(builtRequest);
            return completionResponse;
        }).when(client).filesCompleteUploadExternal(Mockito.<RequestConfigurator<FilesCompleteUploadExternalRequest.FilesCompleteUploadExternalRequestBuilder>>any());

        FilesUploadV2Response result = new FilesUploadV2Helper(client).completeUploads(v2Request, files);

        assertThat(result.isOk(), is(true));
        assertThat(result.getFile().getId(), is("F111"));
        assertThat(result.getFile().getTitle(), is("sample.txt"));

        FilesCompleteUploadExternalRequest actualRequest = capturedRequest.get();
        assertThat(actualRequest.getToken(), is("xoxb-test-token"));
        assertThat(actualRequest.getFiles(), is(files));
        assertThat(actualRequest.getChannelId(), is("C123"));
        assertThat(actualRequest.getChannels(), is(Arrays.asList("C123", "C234")));
        assertThat(actualRequest.getInitialComment(), is("hello"));
        assertThat(actualRequest.getThreadTs(), is("123.456"));
        assertThat(actualRequest.getBlocks(), is(blocks));
        assertThat(actualRequest.getBlocksAsString(), is("[{\"type\":\"section\"}]"));
    }
}
