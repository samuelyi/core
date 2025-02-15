package com.dotmarketing.image.filter;

import static com.dotmarketing.image.focalpoint.FocalPointAPIImpl.TMP;

import com.dotmarketing.util.UUIDUtil;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.image.focalpoint.FocalPoint;
import com.dotmarketing.image.focalpoint.FocalPointAPIImpl;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple2;


/**
 * Filter to apply a focal into a point in a file.
 */
public class FocalPointImageFilter extends ImageFilter {

    private final FocalPointAPIImpl util = new FocalPointAPIImpl();

    @Override
    public String[] getAcceptedParameters() {

        return new String[] {"fp (float,float) specifies the focal point for a file"};
    }

    @Override
    public File runFilter(final File file, final Map<String, String[]> parameters) {

        final Optional<FocalPoint> focalPoint = util.parseFocalPointFromParams(parameters);

        if (!overwrite(file, parameters) || !focalPoint.isPresent()) {
            return file;
        }

        final String inode = parameters.get("assetInodeOrIdentifier")[0];
        final String fieldVar = parameters.get("fieldVarName")[0];
        final String assetId = UUIDUtil.isUUID(inode) ? TMP + inode : inode;
        this.util.writeFocalPoint(assetId, fieldVar, focalPoint.get());

        return file;
    }

    @Override
    public File getResultsFile(final File file, final Map<String, String[]> parameters)  {
        return getResultsFile(file, parameters, UtilMethods.getFileExtension(file.getName()));
    }

}
