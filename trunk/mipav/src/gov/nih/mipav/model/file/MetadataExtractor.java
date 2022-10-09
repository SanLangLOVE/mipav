package gov.nih.mipav.model.file;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.Serializable;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;

import java.math.RoundingMode;






/*
 * Ported to MIPAV by William Gandler
 * Copyright 2002-2019 Drew Noakes and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    https://drewnoakes.com/code/exif/
 *    https://github.com/drewnoakes/metadata-extractor
 */


public class MetadataExtractor {
	
	public MetadataExtractor() {
		
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public @interface NotNull
	{
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public @interface Nullable
	{
	}

	/**
	 * Used to suppress specific code analysis warnings produced by the Findbugs tool.
	 *
	 * @author Andreas Ziermann
	 */
	public @interface SuppressWarnings
	{
	    /**
	     * The name of the warning to be suppressed.
	     * @return The name of the warning to be suppressed.
	     */
	    @NotNull String value();

	    /**
	     * An explanation of why it is valid to suppress the warning in a particular situation/context.
	     * @return An explanation of why it is valid to suppress the warning in a particular situation/context.
	     */
	    @NotNull String justification();
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public final class StringValue
	{
	    @NotNull
	    private final byte[] _bytes;

	    @Nullable
	    private final Charset _charset;

	    public StringValue(@NotNull byte[] bytes, @Nullable Charset charset)
	    {
	        _bytes = bytes;
	        _charset = charset;
	    }

	    @NotNull
	    public byte[] getBytes()
	    {
	        return _bytes;
	    }

	    @Nullable
	    public Charset getCharset()
	    {
	        return _charset;
	    }

	    @Override
	    public String toString()
	    {
	        return toString(_charset);
	    }

	    public String toString(@Nullable Charset charset)
	    {
	        if (charset != null) {
	            try {
	                return new String(_bytes, charset.name());
	            } catch (UnsupportedEncodingException ex) {
	                // fall through
	            }
	        }

	        return new String(_bytes);
	    }
	}
	    
	    /**
	     * @author Drew Noakes https://drewnoakes.com
	     */
	    //@SuppressWarnings("WeakerAccess")
	    public abstract class SequentialReader
	    {
	        // TODO review whether the masks are needed (in both this and RandomAccessReader)

	        private boolean _isMotorolaByteOrder = true;

	        public abstract long getPosition() throws IOException;

	        /**
	         * Gets the next byte in the sequence.
	         *
	         * @return The read byte value
	         */
	        public abstract byte getByte() throws IOException;

	        /**
	         * Returns the required number of bytes from the sequence.
	         *
	         * @param count The number of bytes to be returned
	         * @return The requested bytes
	         */
	        @NotNull
	        public abstract byte[] getBytes(int count) throws IOException;

	        /**
	         * Retrieves bytes, writing them into a caller-provided buffer.
	         * @param buffer The array to write bytes to.
	         * @param offset The starting position within buffer to write to.
	         * @param count The number of bytes to be written.
	         */
	        public abstract void getBytes(@NotNull byte[] buffer, int offset, int count) throws IOException;

	        /**
	         * Skips forward in the sequence. If the sequence ends, an {@link EOFException} is thrown.
	         *
	         * @param n the number of byte to skip. Must be zero or greater.
	         * @throws EOFException the end of the sequence is reached.
	         * @throws IOException an error occurred reading from the underlying source.
	         */
	        public abstract void skip(long n) throws IOException;

	        /**
	         * Skips forward in the sequence, returning a boolean indicating whether the skip succeeded, or whether the sequence ended.
	         *
	         * @param n the number of byte to skip. Must be zero or greater.
	         * @return a boolean indicating whether the skip succeeded, or whether the sequence ended.
	         * @throws IOException an error occurred reading from the underlying source.
	         */
	        public abstract boolean trySkip(long n) throws IOException;

	        /**
	         * Returns an estimate of the number of bytes that can be read (or skipped
	         * over) from this {@link SequentialReader} without blocking by the next
	         * invocation of a method for this input stream. A single read or skip of
	         * this many bytes will not block, but may read or skip fewer bytes.
	         * <p>
	         * Note that while some implementations of {@link SequentialReader} like
	         * {@link SequentialByteArrayReader} will return the total remaining number
	         * of bytes in the stream, others will not. It is never correct to use the
	         * return value of this method to allocate a buffer intended to hold all
	         * data in this stream.
	         *
	         * @return an estimate of the number of bytes that can be read (or skipped
	         *         over) from this {@link SequentialReader} without blocking or
	         *         {@code 0} when it reaches the end of the input stream.
	         */
	        public abstract int available();

	        /**
	         * Sets the endianness of this reader.
	         * <ul>
	         * <li><code>true</code> for Motorola (or big) endianness (also known as network byte order), with MSB before LSB.</li>
	         * <li><code>false</code> for Intel (or little) endianness, with LSB before MSB.</li>
	         * </ul>
	         *
	         * @param motorolaByteOrder <code>true</code> for Motorola/big endian, <code>false</code> for Intel/little endian
	         */
	        public void setMotorolaByteOrder(boolean motorolaByteOrder)
	        {
	            _isMotorolaByteOrder = motorolaByteOrder;
	        }

	        /**
	         * Gets the endianness of this reader.
	         * <ul>
	         * <li><code>true</code> for Motorola (or big) endianness (also known as network byte order), with MSB before LSB.</li>
	         * <li><code>false</code> for Intel (or little) endianness, with LSB before MSB.</li>
	         * </ul>
	         */
	        public boolean isMotorolaByteOrder()
	        {
	            return _isMotorolaByteOrder;
	        }

	        /**
	         * Returns an unsigned 8-bit int calculated from the next byte of the sequence.
	         *
	         * @return the 8 bit int value, between 0 and 255
	         */
	        public short getUInt8() throws IOException
	        {
	            return (short) (getByte() & 0xFF);
	        }

	        /**
	         * Returns a signed 8-bit int calculated from the next byte the sequence.
	         *
	         * @return the 8 bit int value, between 0x00 and 0xFF
	         */
	        public byte getInt8() throws IOException
	        {
	            return getByte();
	        }

	        /**
	         * Returns an unsigned 16-bit int calculated from the next two bytes of the sequence.
	         *
	         * @return the 16 bit int value, between 0x0000 and 0xFFFF
	         */
	        public int getUInt16() throws IOException
	        {
	            if (_isMotorolaByteOrder) {
	                // Motorola - MSB first
	                return (getByte() << 8 & 0xFF00) |
	                       (getByte()      & 0xFF);
	            } else {
	                // Intel ordering - LSB first
	                return (getByte()      & 0xFF) |
	                       (getByte() << 8 & 0xFF00);
	            }
	        }

	        /**
	         * Returns a signed 16-bit int calculated from two bytes of data (MSB, LSB).
	         *
	         * @return the 16 bit int value, between 0x0000 and 0xFFFF
	         * @throws IOException the buffer does not contain enough bytes to service the request
	         */
	        public short getInt16() throws IOException
	        {
	            if (_isMotorolaByteOrder) {
	                // Motorola - MSB first
	                return (short) (((short)getByte() << 8 & (short)0xFF00) |
	                                ((short)getByte()      & (short)0xFF));
	            } else {
	                // Intel ordering - LSB first
	                return (short) (((short)getByte()      & (short)0xFF) |
	                                ((short)getByte() << 8 & (short)0xFF00));
	            }
	        }

	        /**
	         * Get a 32-bit unsigned integer from the buffer, returning it as a long.
	         *
	         * @return the unsigned 32-bit int value as a long, between 0x00000000 and 0xFFFFFFFF
	         * @throws IOException the buffer does not contain enough bytes to service the request
	         */
	        public long getUInt32() throws IOException
	        {
	            if (_isMotorolaByteOrder) {
	                // Motorola - MSB first (big endian)
	                return (((long)getByte()) << 24 & 0xFF000000L) |
	                       (((long)getByte()) << 16 & 0xFF0000L) |
	                       (((long)getByte()) << 8  & 0xFF00L) |
	                       (((long)getByte())       & 0xFFL);
	            } else {
	                // Intel ordering - LSB first (little endian)
	                return (((long)getByte())       & 0xFFL) |
	                       (((long)getByte()) << 8  & 0xFF00L) |
	                       (((long)getByte()) << 16 & 0xFF0000L) |
	                       (((long)getByte()) << 24 & 0xFF000000L);
	            }
	        }

	        /**
	         * Returns a signed 32-bit integer from four bytes of data.
	         *
	         * @return the signed 32 bit int value, between 0x00000000 and 0xFFFFFFFF
	         * @throws IOException the buffer does not contain enough bytes to service the request
	         */
	        public int getInt32() throws IOException
	        {
	            if (_isMotorolaByteOrder) {
	                // Motorola - MSB first (big endian)
	                return (getByte() << 24 & 0xFF000000) |
	                       (getByte() << 16 & 0xFF0000) |
	                       (getByte() << 8  & 0xFF00) |
	                       (getByte()       & 0xFF);
	            } else {
	                // Intel ordering - LSB first (little endian)
	                return (getByte()       & 0xFF) |
	                       (getByte() << 8  & 0xFF00) |
	                       (getByte() << 16 & 0xFF0000) |
	                       (getByte() << 24 & 0xFF000000);
	            }
	        }

	        /**
	         * Get a signed 64-bit integer from the buffer.
	         *
	         * @return the 64 bit int value, between 0x0000000000000000 and 0xFFFFFFFFFFFFFFFF
	         * @throws IOException the buffer does not contain enough bytes to service the request
	         */
	        public long getInt64() throws IOException
	        {
	            if (_isMotorolaByteOrder) {
	                // Motorola - MSB first
	                return ((long)getByte() << 56 & 0xFF00000000000000L) |
	                       ((long)getByte() << 48 & 0xFF000000000000L) |
	                       ((long)getByte() << 40 & 0xFF0000000000L) |
	                       ((long)getByte() << 32 & 0xFF00000000L) |
	                       ((long)getByte() << 24 & 0xFF000000L) |
	                       ((long)getByte() << 16 & 0xFF0000L) |
	                       ((long)getByte() << 8  & 0xFF00L) |
	                       ((long)getByte()       & 0xFFL);
	            } else {
	                // Intel ordering - LSB first
	                return ((long)getByte()       & 0xFFL) |
	                       ((long)getByte() << 8  & 0xFF00L) |
	                       ((long)getByte() << 16 & 0xFF0000L) |
	                       ((long)getByte() << 24 & 0xFF000000L) |
	                       ((long)getByte() << 32 & 0xFF00000000L) |
	                       ((long)getByte() << 40 & 0xFF0000000000L) |
	                       ((long)getByte() << 48 & 0xFF000000000000L) |
	                       ((long)getByte() << 56 & 0xFF00000000000000L);
	            }
	        }

	        /**
	         * Gets a s15.16 fixed point float from the buffer.
	         * <p>
	         * This particular fixed point encoding has one sign bit, 15 numerator bits and 16 denominator bits.
	         *
	         * @return the floating point value
	         * @throws IOException the buffer does not contain enough bytes to service the request
	         */
	        public float getS15Fixed16() throws IOException
	        {
	            if (_isMotorolaByteOrder) {
	                float res = (getByte() & 0xFF) << 8 |
	                            (getByte() & 0xFF);
	                int d =     (getByte() & 0xFF) << 8 |
	                            (getByte() & 0xFF);
	                return (float)(res + d/65536.0);
	            } else {
	                // this particular branch is untested
	                int d =     (getByte() & 0xFF) |
	                            (getByte() & 0xFF) << 8;
	                float res = (getByte() & 0xFF) |
	                            (getByte() & 0xFF) << 8;
	                return (float)(res + d/65536.0);
	            }
	        }

	        public float getFloat32() throws IOException
	        {
	            return Float.intBitsToFloat(getInt32());
	        }

	        public double getDouble64() throws IOException
	        {
	            return Double.longBitsToDouble(getInt64());
	        }

	        @NotNull
	        public String getString(int bytesRequested) throws IOException
	        {
	            return new String(getBytes(bytesRequested));
	        }

	        @NotNull
	        public String getString(int bytesRequested, String charset) throws IOException
	        {
	            byte[] bytes = getBytes(bytesRequested);
	            try {
	                return new String(bytes, charset);
	            } catch (UnsupportedEncodingException e) {
	                return new String(bytes);
	            }
	        }

	        @NotNull
	        public String getString(int bytesRequested, @NotNull Charset charset) throws IOException
	        {
	            byte[] bytes = getBytes(bytesRequested);
	            return new String(bytes, charset);
	        }

	        @NotNull
	        public StringValue getStringValue(int bytesRequested, @Nullable Charset charset) throws IOException
	        {
	            return new StringValue(getBytes(bytesRequested), charset);
	        }

	        /**
	         * Creates a String from the stream, ending where <code>byte=='\0'</code> or where <code>length==maxLength</code>.
	         *
	         * @param maxLengthBytes The maximum number of bytes to read.  If a zero-byte is not reached within this limit,
	         *                       reading will stop and the string will be truncated to this length.
	         * @return The read string.
	         * @throws IOException The buffer does not contain enough bytes to satisfy this request.
	         */
	        @NotNull
	        public String getNullTerminatedString(int maxLengthBytes, Charset charset) throws IOException
	        {
	           return getNullTerminatedStringValue(maxLengthBytes, charset).toString();
	        }

	        /**
	         * Creates a String from the stream, ending where <code>byte=='\0'</code> or where <code>length==maxLength</code>.
	         *
	         * @param maxLengthBytes The maximum number of bytes to read.  If a <code>\0</code> byte is not reached within this limit,
	         *                       reading will stop and the string will be truncated to this length.
	         * @param charset The <code>Charset</code> to register with the returned <code>StringValue</code>, or <code>null</code> if the encoding
	         *                is unknown
	         * @return The read string.
	         * @throws IOException The buffer does not contain enough bytes to satisfy this request.
	         */
	        @NotNull
	        public StringValue getNullTerminatedStringValue(int maxLengthBytes, Charset charset) throws IOException
	        {
	            byte[] bytes = getNullTerminatedBytes(maxLengthBytes);

	            return new StringValue(bytes, charset);
	        }

	        /**
	         * Returns the sequence of bytes punctuated by a <code>\0</code> value.
	         *
	         * @param maxLengthBytes The maximum number of bytes to read. If a <code>\0</code> byte is not reached within this limit,
	         * the returned array will be <code>maxLengthBytes</code> long.
	         * @return The read byte array, excluding the null terminator.
	         * @throws IOException The buffer does not contain enough bytes to satisfy this request.
	         */
	        @NotNull
	        public byte[] getNullTerminatedBytes(int maxLengthBytes) throws IOException
	        {
	            byte[] buffer = new byte[maxLengthBytes];

	            // Count the number of non-null bytes
	            int length = 0;
	            while (length < buffer.length && (buffer[length] = getByte()) != 0)
	                length++;

	            if (length == maxLengthBytes)
	                return buffer;

	            byte[] bytes = new byte[length];
	            if (length > 0)
	                System.arraycopy(buffer, 0, bytes, 0, length);
	            return bytes;
	        }
	    }


	
	/**
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class StreamReader extends SequentialReader
	{
	    @NotNull
	    private final InputStream _stream;

	    private long _pos;

	    @Override
	    public long getPosition()
	    {
	        return _pos;
	    }

	    //@SuppressWarnings("ConstantConditions")
	    public StreamReader(@NotNull InputStream stream)
	    {
	        if (stream == null)
	            throw new NullPointerException();

	        _stream = stream;
	        _pos = 0;
	    }

	    @Override
	    public byte getByte() throws IOException
	    {
	        int value = _stream.read();
	        if (value == -1)
	            throw new EOFException("End of data reached.");
	        _pos++;
	        return (byte)value;
	    }

	    @NotNull
	    @Override
	    public byte[] getBytes(int count) throws IOException
	    {
	        try {
	            byte[] bytes = new byte[count];
	            getBytes(bytes, 0, count);
	            return bytes;
	        } catch (OutOfMemoryError e) {
	            throw new EOFException("End of data reached.");
	        }

	    }

	    @Override
	    public void getBytes(@NotNull byte[] buffer, int offset, int count) throws IOException
	    {
	        int totalBytesRead = 0;
	        while (totalBytesRead != count)
	        {
	            final int bytesRead = _stream.read(buffer, offset + totalBytesRead, count - totalBytesRead);
	            if (bytesRead == -1)
	                throw new EOFException("End of data reached.");
	            totalBytesRead += bytesRead;
	            assert(totalBytesRead <= count);
	        }
	        _pos += totalBytesRead;
	    }

	    @Override
	    public void skip(long n) throws IOException
	    {
	        if (n < 0)
	            throw new IllegalArgumentException("n must be zero or greater.");

	        long skippedCount = skipInternal(n);

	        if (skippedCount != n)
	            throw new EOFException(String.format("Unable to skip. Requested %d bytes but only %d remained.", n, skippedCount));
	    }

	    @Override
	    public boolean trySkip(long n) throws IOException
	    {
	        if (n < 0)
	            throw new IllegalArgumentException("n must be zero or greater.");

	        return skipInternal(n) == n;
	    }

	    @Override
	    public int available() {
	        try {
	            return _stream.available();
	        } catch (IOException e) {
	            return 0;
	        }
	    }

	    private long skipInternal(long n) throws IOException
	    {
	        // It seems that for some streams, such as BufferedInputStream, that skip can return
	        // some smaller number than was requested. So loop until we either skip enough, or
	        // InputStream.skip returns zero.
	        //
	        // See http://stackoverflow.com/questions/14057720/robust-skipping-of-data-in-a-java-io-inputstream-and-its-subtypes
	        //
	        long skippedTotal = 0;
	        while (skippedTotal != n) {
	            long skipped = _stream.skip(n - skippedTotal);
	            skippedTotal += skipped;
	            if (skipped == 0)
	                break;
	        }
	        _pos += skippedTotal;
	        return skippedTotal;
	    }
	}
	
	/**
	 * Immutable class for holding a rational number without loss of precision.  Provides
	 * a familiar representation via {@link Rational#toString} in form <code>numerator/denominator</code>.
	 *
	 * Note that any value with a numerator of zero will be treated as zero, even if the
	 * denominator is also zero.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class Rational extends java.lang.Number implements Comparable<Rational>, Serializable
	{
	    private static final long serialVersionUID = 510688928138848770L;

	    /** Holds the numerator. */
	    private final long _numerator;

	    /** Holds the denominator. */
	    private final long _denominator;

	    /**
	     * Creates a new instance of Rational.  Rational objects are immutable, so
	     * once you've set your numerator and denominator values here, you're stuck
	     * with them!
	     */
	    public Rational(long numerator, long denominator)
	    {
	        _numerator = numerator;
	        _denominator = denominator;
	    }

	    /**
	     * Returns the value of the specified number as a <code>double</code>.
	     * This may involve rounding.
	     *
	     * @return the numeric value represented by this object after conversion
	     *         to type <code>double</code>.
	     */
	    @Override
	    public double doubleValue()
	    {
	        return _numerator == 0
	            ? 0.0
	            : (double) _numerator / (double) _denominator;
	    }

	    /**
	     * Returns the value of the specified number as a <code>float</code>.
	     * This may involve rounding.
	     *
	     * @return the numeric value represented by this object after conversion
	     *         to type <code>float</code>.
	     */
	    @Override
	    public float floatValue()
	    {
	        return _numerator == 0
	            ? 0.0f
	            : (float) _numerator / (float) _denominator;
	    }

	    /**
	     * Returns the value of the specified number as a <code>byte</code>.
	     * This may involve rounding or truncation.  This implementation simply
	     * casts the result of {@link Rational#doubleValue} to <code>byte</code>.
	     *
	     * @return the numeric value represented by this object after conversion
	     *         to type <code>byte</code>.
	     */
	    @Override
	    public final byte byteValue()
	    {
	        return (byte) doubleValue();
	    }

	    /**
	     * Returns the value of the specified number as an <code>int</code>.
	     * This may involve rounding or truncation.  This implementation simply
	     * casts the result of {@link Rational#doubleValue} to <code>int</code>.
	     *
	     * @return the numeric value represented by this object after conversion
	     *         to type <code>int</code>.
	     */
	    @Override
	    public final int intValue()
	    {
	        return (int) doubleValue();
	    }

	    /**
	     * Returns the value of the specified number as a <code>long</code>.
	     * This may involve rounding or truncation.  This implementation simply
	     * casts the result of {@link Rational#doubleValue} to <code>long</code>.
	     *
	     * @return the numeric value represented by this object after conversion
	     *         to type <code>long</code>.
	     */
	    @Override
	    public final long longValue()
	    {
	        return (long) doubleValue();
	    }

	    /**
	     * Returns the value of the specified number as a <code>short</code>.
	     * This may involve rounding or truncation.  This implementation simply
	     * casts the result of {@link Rational#doubleValue} to <code>short</code>.
	     *
	     * @return the numeric value represented by this object after conversion
	     *         to type <code>short</code>.
	     */
	    @Override
	    public final short shortValue()
	    {
	        return (short) doubleValue();
	    }


	    /** Returns the denominator. */
	    public final long getDenominator()
	    {
	        return this._denominator;
	    }

	    /** Returns the numerator. */
	    public final long getNumerator()
	    {
	        return this._numerator;
	    }

	    /**
	     * Returns the reciprocal value of this object as a new Rational.
	     *
	     * @return the reciprocal in a new object
	     */
	    @NotNull
	    public Rational getReciprocal()
	    {
	        return new Rational(this._denominator, this._numerator);
	    }

	    /**
	     * Returns the absolute value of this object as a new Rational.
	     *
	     * @return the absolute value in a new object
	     */
	    public Rational getAbsolute()
	    {
	        return new Rational(Math.abs(this._numerator), Math.abs(this._denominator));
	    }

	    /** Checks if this {@link Rational} number is an Integer, either positive or negative. */
	    public boolean isInteger()
	    {
	        return _denominator == 1 ||
	                (_denominator != 0 && (_numerator % _denominator == 0)) ||
	                (_denominator == 0 && _numerator == 0);
	    }

	    /** Checks if either the numerator or denominator are zero. */
	    public boolean isZero()
	    {
	        return _numerator == 0 || _denominator == 0;
	    }

	    /** True if the value is non-zero and numerator and denominator are either both positive or both negative. */
	    public boolean isPositive()
	    {
	        return !isZero() && (_numerator > 0 == _denominator > 0);
	    }

	    /**
	     * Returns a string representation of the object of form <code>numerator/denominator</code>.
	     *
	     * @return a string representation of the object.
	     */
	    @Override
	    @NotNull
	    public String toString()
	    {
	        return _numerator + "/" + _denominator;
	    }

	    /** Returns the simplest representation of this {@link Rational}'s value possible. */
	    @NotNull
	    public String toSimpleString(boolean allowDecimal)
	    {
	        if (_denominator == 0 && _numerator != 0) {
	            return toString();
	        } else if (isInteger()) {
	            return Integer.toString(intValue());
	        } else {
	            Rational simplifiedInstance = getSimplifiedInstance();
	            if (allowDecimal) {
	                String doubleString = Double.toString(simplifiedInstance.doubleValue());
	                if (doubleString.length() < 5) {
	                    return doubleString;
	                }
	            }
	            return simplifiedInstance.toString();
	        }
	    }

	    /**
	     * Compares two {@link Rational} instances, returning true if they are mathematically
	     * equivalent (in consistence with {@link Rational#equals(Object)} method).
	     *
	     * @param that the {@link Rational} to compare this instance to.
	     * @return the value {@code 0} if this {@link Rational} is
	     *         equal to the argument {@link Rational} mathematically; a value less
	     *         than {@code 0} if this {@link Rational} is less
	     *         than the argument {@link Rational}; and a value greater
	     *         than {@code 0} if this {@link Rational} is greater than the argument
	     *         {@link Rational}.
	     */
	    public int compareTo(@NotNull Rational that) {
	        return Double.compare(this.doubleValue(), that.doubleValue());
	    }

	    /**
	     * Indicates whether this instance and <code>other</code> are numerically equal,
	     * even if their representations differ.
	     *
	     * For example, 1/2 is equal to 10/20 by this method.
	     * Similarly, 1/0 is equal to 100/0 by this method.
	     * To test equal representations, use EqualsExact.
	     *
	     * @param other The rational value to compare with
	     */
	    public boolean equals(Rational other) {
	        return other.doubleValue() == doubleValue();
	    }

	    /**
	     * Indicates whether this instance and <code>other</code> have identical
	     * Numerator and Denominator.
	     * <p>
	     * For example, 1/2 is not equal to 10/20 by this method.
	     * Similarly, 1/0 is not equal to 100/0 by this method.
	     * To test numerically equivalence, use Equals(Rational).</p>
	     *
	     * @param other The rational value to compare with
	     */
	    public boolean equalsExact(Rational other) {
	        return getDenominator() == other.getDenominator() && getNumerator() == other.getNumerator();
	    }

	    /**
	     * Compares two {@link Rational} instances, returning true if they are mathematically
	     * equivalent.
	     *
	     * @param obj the {@link Rational} to compare this instance to.
	     * @return true if instances are mathematically equivalent, otherwise false.  Will also
	     *         return false if <code>obj</code> is not an instance of {@link Rational}.
	     */
	    @Override
	    public boolean equals(@Nullable Object obj)
	    {
	        if (!(obj instanceof Rational))
	            return false;
	        Rational that = (Rational) obj;
	        return this.doubleValue() == that.doubleValue();
	    }

	    @Override
	    public int hashCode()
	    {
	        return (23 * (int)_denominator) + (int)_numerator;
	    }

	    /**
	     * <p>
	     * Simplifies the representation of this {@link Rational} number.</p>
	     * <p>
	     * For example, 5/10 simplifies to 1/2 because both Numerator
	     * and Denominator share a common factor of 5.</p>
	     * <p>
	     * Uses the Euclidean Algorithm to find the greatest common divisor.</p>
	     *
	     * @return A simplified instance if one exists, otherwise a copy of the original value.
	     */
	    @NotNull
	    public Rational getSimplifiedInstance()
	    {
	        long n = _numerator;
	        long d = _denominator;

	        if (d < 0) {
	            n = -n;
	            d = -d;
	        }

	        long gcd = GCD(n, d);

	        return new Rational(n / gcd, d / gcd);
	    }

	    private long GCD(long a, long b)
	    {
	        if (a < 0)
	            a = -a;
	        if (b < 0)
	            b = -b;

	        while (a != 0 && b != 0)
	        {
	            if (a > b)
	                a %= b;
	            else
	                b %= a;
	        }

	        return a == 0 ? b : a;
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public final static class StringUtil
	{
	    @NotNull
	    public static String join(@NotNull Iterable<? extends CharSequence> strings, @NotNull String delimiter)
	    {
	        int capacity = 0;
	        int delimLength = delimiter.length();

	        Iterator<? extends CharSequence> iter = strings.iterator();
	        if (iter.hasNext())
	            capacity += iter.next().length() + delimLength;

	        StringBuilder buffer = new StringBuilder(capacity);
	        iter = strings.iterator();
	        if (iter.hasNext()) {
	            buffer.append(iter.next());
	            while (iter.hasNext()) {
	                buffer.append(delimiter);
	                buffer.append(iter.next());
	            }
	        }
	        return buffer.toString();
	    }

	    @NotNull
	    public static <T extends CharSequence> String join(@NotNull T[] strings, @NotNull String delimiter)
	    {
	        int capacity = 0;
	        int delimLength = delimiter.length();
	        for (T value : strings)
	            capacity += value.length() + delimLength;

	        StringBuilder buffer = new StringBuilder(capacity);
	        boolean first = true;
	        for (T value : strings) {
	            if (!first) {
	                buffer.append(delimiter);
	            } else {
	                first = false;
	            }
	            buffer.append(value);
	        }
	        return buffer.toString();
	    }

	    @NotNull
	    public static String fromStream(@NotNull InputStream stream) throws IOException
	    {
	        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
	        StringBuilder sb = new StringBuilder();
	        String line;
	        while ((line = reader.readLine()) != null) {
	            sb.append(line);
	        }
	        return sb.toString();
	    }

	    public static int compare(@Nullable String s1, @Nullable String s2)
	    {
	        boolean null1 = s1 == null;
	        boolean null2 = s2 == null;

	        if (null1 && null2) {
	            return 0;
	        } else if (null1) {
	            return -1;
	        } else if (null2) {
	            return 1;
	        } else {
	            return s1.compareTo(s2);
	        }
	    }

	    @NotNull
	    public static String urlEncode(@NotNull String name)
	    {
	        // Sufficient for now, it seems
	        return name.replace(" ", "%20");
	    }
	}

	
	/**
	 * Models a particular tag within a {@link com.drew.metadata.Directory} and provides methods for obtaining its value.
	 * Immutable.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("unused")
	public class Tag
	{
	    private final int _tagType;
	    @NotNull
	    private final Directory _directory;

	    public Tag(int tagType, @NotNull Directory directory)
	    {
	        _tagType = tagType;
	        _directory = directory;
	    }

	    /**
	     * Gets the tag type as an int
	     *
	     * @return the tag type as an int
	     */
	    public int getTagType()
	    {
	        return _tagType;
	    }

	    /**
	     * Gets the tag type in hex notation as a String with padded leading
	     * zeroes if necessary (i.e. <code>0x100e</code>).
	     *
	     * @return the tag type as a string in hexadecimal notation
	     */
	    @NotNull
	    public String getTagTypeHex()
	    {
	        return String.format("0x%04x", _tagType);
	    }

	    /**
	     * Get a description of the tag's value, considering enumerated values
	     * and units.
	     *
	     * @return a description of the tag's value
	     */
	    @Nullable
	    public String getDescription()
	    {
	        return _directory.getDescription(_tagType);
	    }

	    /**
	     * Get whether this tag has a name.
	     *
	     * If <code>true</code>, it may be accessed via {@link #getTagName}.
	     * If <code>false</code>, {@link #getTagName} will return a string resembling <code>"Unknown tag (0x1234)"</code>.
	     *
	     * @return whether this tag has a name
	     */
	    public boolean hasTagName()
	    {
	        return _directory.hasTagName(_tagType);
	    }

	    /**
	     * Get the name of the tag, such as <code>Aperture</code>, or
	     * <code>InteropVersion</code>.
	     *
	     * @return the tag's name
	     */
	    @NotNull
	    public String getTagName()
	    {
	        return _directory.getTagName(_tagType);
	    }

	    /**
	     * Get the name of the {@link com.drew.metadata.Directory} in which the tag exists, such as
	     * <code>Exif</code>, <code>GPS</code> or <code>Interoperability</code>.
	     *
	     * @return name of the {@link com.drew.metadata.Directory} in which this tag exists
	     */
	    @NotNull
	    public String getDirectoryName()
	    {
	        return _directory.getName();
	    }

	    /**
	     * A basic representation of the tag's type and value.  EG: <code>[Exif IFD0] FNumber - f/2.8</code>.
	     *
	     * @return the tag's type and value
	     */
	    @Override
	    @NotNull
	    public String toString()
	    {
	        String description = getDescription();
	        if (description == null)
	            description = _directory.getString(getTagType()) + " (unable to formulate description)";
	        return "[" + _directory.getName() + "] " + getTagName() + " - " + description;
	    }
	}
	
	/**
	 * Base class for all tag descriptor classes.  Implementations are responsible for
	 * providing the human-readable string representation of tag values stored in a directory.
	 * The directory is provided to the tag descriptor via its constructor.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class TagDescriptor<T extends Directory>
	{
	    @NotNull
	    protected final T _directory;

	    public TagDescriptor(@NotNull T directory)
	    {
	        _directory = directory;
	    }

	    /**
	     * Returns a descriptive value of the specified tag for this image.
	     * Where possible, known values will be substituted here in place of the raw
	     * tokens actually kept in the metadata segment.  If no substitution is
	     * available, the value provided by <code>getString(tagType)</code> will be returned.
	     *
	     * @param tagType the tag to find a description for
	     * @return a description of the image's value for the specified tag, or
	     *         <code>null</code> if the tag hasn't been defined.
	     */
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        Object object = _directory.getObject(tagType);

	        if (object == null)
	            return null;

	        // special presentation for long arrays
	        if (object.getClass().isArray()) {
	            final int length = Array.getLength(object);
	            if (length > 16) {
	                return String.format("[%d values]", length);
	            }
	        }

	        if (object instanceof Date) {
	            // Produce a date string having a format that includes the offset in form "+00:00"
	            return new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")
	                .format((Date) object)
	                .replaceAll("([0-9]{2} [^ ]+)$", ":$1");
	        }

	        // no special handling required, so use default conversion to a string
	        return _directory.getString(tagType);
	    }

	    /**
	     * Takes a series of 4 bytes from the specified offset, and converts these to a
	     * well-known version number, where possible.
	     * <p>
	     * Two different formats are processed:
	     * <ul>
	     * <li>[30 32 31 30] -&gt; 2.10</li>
	     * <li>[0 1 0 0] -&gt; 1.00</li>
	     * </ul>
	     *
	     * @param components  the four version values
	     * @param majorDigits the number of components to be
	     * @return the version as a string of form "2.10" or null if the argument cannot be converted
	     */
	    @Nullable
	    public String convertBytesToVersionString(@Nullable int[] components, final int majorDigits)
	    {
	        if (components == null)
	            return null;
	        StringBuilder version = new StringBuilder();
	        for (int i = 0; i < 4 && i < components.length; i++) {
	            if (i == majorDigits)
	                version.append('.');
	            char c = (char)components[i];
	            if (c < '0')
	                c += '0';
	            if (i == 0 && c == '0')
	                continue;
	            version.append(c);
	        }
	        return version.toString();
	    }

	    @Nullable
	    protected String getVersionBytesDescription(final int tagType, int majorDigits)
	    {
	        int[] values = _directory.getIntArray(tagType);
	        return values == null ? null : convertBytesToVersionString(values, majorDigits);
	    }

	    @Nullable
	    protected String getIndexedDescription(final int tagType, @NotNull String... descriptions)
	    {
	        return getIndexedDescription(tagType, 0, descriptions);
	    }

	    @Nullable
	    protected String getIndexedDescription(final int tagType, final int baseIndex, @NotNull String... descriptions)
	    {
	        final Long index = _directory.getLongObject(tagType);
	        if (index == null)
	            return null;
	        final long arrayIndex = index - baseIndex;
	        if (arrayIndex >= 0 && arrayIndex < (long)descriptions.length) {
	            String description = descriptions[(int)arrayIndex];
	            if (description != null)
	                return description;
	        }
	        return "Unknown (" + index + ")";
	    }

	    @Nullable
	    protected String getByteLengthDescription(final int tagType)
	    {
	        byte[] bytes = _directory.getByteArray(tagType);
	        if (bytes == null)
	            return null;
	        return String.format("(%d byte%s)", bytes.length, bytes.length == 1 ? "" : "s");
	    }

	    @Nullable
	    protected String getSimpleRational(final int tagType)
	    {
	        Rational value = _directory.getRational(tagType);
	        if (value == null)
	            return null;
	        return value.toSimpleString(true);
	    }

	    @Nullable
	    protected String getDecimalRational(final int tagType, final int decimalPlaces)
	    {
	        Rational value = _directory.getRational(tagType);
	        if (value == null)
	            return null;
	        return String.format("%." + decimalPlaces + "f", value.doubleValue());
	    }

	    @Nullable
	    protected String getFormattedInt(final int tagType, @NotNull final String format)
	    {
	        Integer value = _directory.getInteger(tagType);
	        if (value == null)
	            return null;
	        return String.format(format, value);
	    }

	    @Nullable
	    protected String getFormattedFloat(final int tagType, @NotNull final String format)
	    {
	        Float value = _directory.getFloatObject(tagType);
	        if (value == null)
	            return null;
	        return String.format(format, value);
	    }

	    @Nullable
	    protected String getFormattedString(final int tagType, @NotNull final String format)
	    {
	        String value = _directory.getString(tagType);
	        if (value == null)
	            return null;
	        return String.format(format, value);
	    }

	    @Nullable
	    protected String getEpochTimeDescription(final int tagType)
	    {
	        // TODO have observed a byte[8] here which is likely some kind of date (ticks as long?)
	        Long value = _directory.getLongObject(tagType);
	        if (value == null)
	            return null;
	        return new Date(value).toString();
	    }

	    /**
	     * LSB first. Labels may be null, a String, or a String[2] with (low label,high label) values.
	     */
	    @Nullable
	    protected String getBitFlagDescription(final int tagType, @NotNull final Object... labels)
	    {
	        Integer value = _directory.getInteger(tagType);

	        if (value == null)
	            return null;

	        List<String> parts = new ArrayList<String>();

	        int bitIndex = 0;
	        while (labels.length > bitIndex) {
	            Object labelObj = labels[bitIndex];
	            if (labelObj != null) {
	                boolean isBitSet = (value & 1) == 1;
	                if (labelObj instanceof String[]) {
	                    String[] labelPair = (String[])labelObj;
	                    assert(labelPair.length == 2);
	                    parts.add(labelPair[isBitSet ? 1 : 0]);
	                } else if (isBitSet && labelObj instanceof String) {
	                    parts.add((String)labelObj);
	                }
	            }
	            value >>= 1;
	            bitIndex++;
	        }

	        return StringUtil.join(parts, ", ");
	    }

	    @Nullable
	    protected String get7BitStringFromBytes(final int tagType)
	    {
	        final byte[] bytes = _directory.getByteArray(tagType);

	        if (bytes == null)
	            return null;

	        int length = bytes.length;
	        for (int index = 0; index < bytes.length; index++) {
	            int i = bytes[index] & 0xFF;
	            if (i == 0 || i > 0x7F) {
	                length = index;
	                break;
	            }
	        }

	        return new String(bytes, 0, length);
	    }

	    @Nullable
	    protected String getStringFromBytes(int tag, Charset cs)
	    {
	        byte[] values = _directory.getByteArray(tag);

	        if (values == null)
	            return null;

	        try {
	            return new String(values, cs.name()).trim();
	        } catch (UnsupportedEncodingException e) {
	            return null;
	        }
	    }

	    @Nullable
	    protected String getRationalOrDoubleString(int tagType)
	    {
	        Rational rational = _directory.getRational(tagType);
	        if (rational != null)
	            return rational.toSimpleString(true);

	        Double d = _directory.getDoubleObject(tagType);
	        if (d != null) {
	            DecimalFormat format = new DecimalFormat("0.###");
	            return format.format(d);
	        }

	        return null;
	    }

	    @NotNull
	    protected String getFStopDescription(double fStop)
	    {
	        DecimalFormat format = new DecimalFormat("0.0");
	        format.setRoundingMode(RoundingMode.HALF_UP);
	        return "f/" + format.format(fStop);
	    }

	    @NotNull
	    protected String getFocalLengthDescription(double mm)
	    {
	        DecimalFormat format = new DecimalFormat("0.#");
	        format.setRoundingMode(RoundingMode.HALF_UP);
	        return format.format(mm) + " mm";
	    }

	    @Nullable
	    protected String getLensSpecificationDescription(int tag)
	    {
	        Rational[] values = _directory.getRationalArray(tag);

	        if (values == null || values.length != 4 || (values[0].isZero() && values[2].isZero()))
	            return null;

	        StringBuilder sb = new StringBuilder();

	        if (values[0].equals(values[1]))
	            sb.append(values[0].toSimpleString(true)).append("mm");
	        else
	            sb.append(values[0].toSimpleString(true)).append('-').append(values[1].toSimpleString(true)).append("mm");

	        if (!values[2].isZero()) {
	            sb.append(' ');

	            DecimalFormat format = new DecimalFormat("0.0");
	            format.setRoundingMode(RoundingMode.HALF_UP);

	            if (values[2].equals(values[3]))
	                sb.append(getFStopDescription(values[2].doubleValue()));
	            else
	                sb.append("f/").append(format.format(values[2].doubleValue())).append('-').append(format.format(values[3].doubleValue()));
	        }

	        return sb.toString();
	    }

	    @Nullable
	    protected String getOrientationDescription(int tag)
	    {
	        return getIndexedDescription(tag, 1,
	            "Top, left side (Horizontal / normal)",
	            "Top, right side (Mirror horizontal)",
	            "Bottom, right side (Rotate 180)",
	            "Bottom, left side (Mirror vertical)",
	            "Left side, top (Mirror horizontal and rotate 270 CW)",
	            "Right side, top (Rotate 90 CW)",
	            "Right side, bottom (Mirror horizontal and rotate 90 CW)",
	            "Left side, bottom (Rotate 270 CW)");
	    }

	    @Nullable
	    protected String getShutterSpeedDescription(int tag)
	    {
	        // Thanks to Mark Edwards for spotting and patching a bug in the calculation of this
	        // description (spotted bug using a Canon EOS 300D).
	        // Thanks also to Gli Blr for spotting this bug.
	        Float apexValue = _directory.getFloatObject(tag);
	        if (apexValue == null)
	            return null;
	        if (apexValue <= 1) {
	            float apexPower = (float)(1 / (Math.exp(apexValue * Math.log(2))));
	            long apexPower10 = Math.round((double)apexPower * 10.0);
	            float fApexPower = (float)apexPower10 / 10.0f;
	            DecimalFormat format = new DecimalFormat("0.##");
	            format.setRoundingMode(RoundingMode.HALF_UP);
	            return format.format(fApexPower) + " sec";
	        } else {
	            int apexPower = (int)((Math.exp(apexValue * Math.log(2))));
	            return "1/" + apexPower + " sec";
	        }
	    }

	    // EXIF UserComment, GPSProcessingMethod and GPSAreaInformation
	    @Nullable
	    protected String getEncodedTextDescription(int tagType)
	    {
	        byte[] commentBytes = _directory.getByteArray(tagType);
	        if (commentBytes == null)
	            return null;
	        if (commentBytes.length == 0)
	            return "";

	        final Map<String, String> encodingMap = new HashMap<String, String>();
	        encodingMap.put("ASCII", System.getProperty("file.encoding")); // Someone suggested "ISO-8859-1".
	        encodingMap.put("UNICODE", "UTF-16LE");
	        encodingMap.put("JIS", "Shift-JIS"); // We assume this charset for now.  Another suggestion is "JIS".

	        try {
	            if (commentBytes.length >= 10) {
	                String firstTenBytesString = new String(commentBytes, 0, 10);

	                // try each encoding name
	                for (Map.Entry<String, String> pair : encodingMap.entrySet()) {
	                    String encodingName = pair.getKey();
	                    String charset = pair.getValue();
	                    if (firstTenBytesString.startsWith(encodingName)) {
	                        // skip any null or blank characters commonly present after the encoding name, up to a limit of 10 from the start
	                        for (int j = encodingName.length(); j < 10; j++) {
	                            byte b = commentBytes[j];
	                            if (b != '\0' && b != ' ')
	                                return new String(commentBytes, j, commentBytes.length - j, charset).trim();
	                        }
	                        return new String(commentBytes, 10, commentBytes.length - 10, charset).trim();
	                    }
	                }
	            }
	            // special handling fell through, return a plain string representation
	            return new String(commentBytes, System.getProperty("file.encoding")).trim();
	        } catch (UnsupportedEncodingException ex) {
	            return null;
	        }
	    }
	}
	
	/**
	 * Represents a compound exception, as modelled in JDK 1.4, but
	 * unavailable in previous versions.  This class allows support
	 * of these previous JDK versions.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class CompoundException extends Exception
	{
	    private static final long serialVersionUID = -9207883813472069925L;

	    @Nullable
	    private final Throwable _innerException;

	    public CompoundException(@Nullable String msg)
	    {
	        this(msg, null);
	    }

	    public CompoundException(@Nullable Throwable exception)
	    {
	        this(null, exception);
	    }

	    public CompoundException(@Nullable String msg, @Nullable Throwable innerException)
	    {
	        super(msg);
	        _innerException = innerException;
	    }

	    @Nullable
	    public Throwable getInnerException()
	    {
	        return _innerException;
	    }

	    @Override
	    @NotNull
	    public String toString()
	    {
	        StringBuilder string = new StringBuilder();
	        string.append(super.toString());
	        if (_innerException != null) {
	            string.append("\n");
	            string.append("--- inner exception ---");
	            string.append("\n");
	            string.append(_innerException.toString());
	        }
	        return string.toString();
	    }

	    @Override
	    public void printStackTrace(@NotNull PrintStream s)
	    {
	        super.printStackTrace(s);
	        if (_innerException != null) {
	            s.println("--- inner exception ---");
	            _innerException.printStackTrace(s);
	        }
	    }

	    @Override
	    public void printStackTrace(@NotNull PrintWriter s)
	    {
	        super.printStackTrace(s);
	        if (_innerException != null) {
	            s.println("--- inner exception ---");
	            _innerException.printStackTrace(s);
	        }
	    }

	    @Override
	    public void printStackTrace()
	    {
	        super.printStackTrace();
	        if (_innerException != null) {
	            System.err.println("--- inner exception ---");
	            _innerException.printStackTrace();
	        }
	    }
	}


	/**
	 * Base class for all metadata specific exceptions.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class MetadataException extends CompoundException
	{
	    private static final long serialVersionUID = 8612756143363919682L;

	    public MetadataException(@Nullable String msg)
	    {
	        super(msg);
	    }

	    public MetadataException(@Nullable Throwable exception)
	    {
	        super(exception);
	    }

	    public MetadataException(@Nullable String msg, @Nullable Throwable innerException)
	    {
	        super(msg, innerException);
	    }
	}

	
	/**
	 * Abstract base class for all directory implementations, having methods for getting and setting tag values of various
	 * data types.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	@java.lang.SuppressWarnings("WeakerAccess")
	public abstract class Directory
	{
	    private static final String _floatFormatPattern = "0.###";

	    /** Map of values hashed by type identifiers. */
	    @NotNull
	    protected final Map<Integer, Object> _tagMap = new HashMap<Integer, Object>();

	    /**
	     * A convenient list holding tag values in the order in which they were stored.
	     * This is used for creation of an iterator, and for counting the number of
	     * defined tags.
	     */
	    @NotNull
	    protected final Collection<Tag> _definedTagList = new ArrayList<Tag>();

	    @NotNull
	    private final Collection<String> _errorList = new ArrayList<String>(4);

	    /** The descriptor used to interpret tag values. */
	    protected TagDescriptor<?> _descriptor;

	    @Nullable
	    private Directory _parent;

	// ABSTRACT METHODS

	    /**
	     * Provides the name of the directory, for display purposes.  E.g. <code>Exif</code>
	     *
	     * @return the name of the directory
	     */
	    @NotNull
	    public abstract String getName();

	    /**
	     * Provides the map of tag names, hashed by tag type identifier.
	     *
	     * @return the map of tag names
	     */
	    @NotNull
	    protected abstract HashMap<Integer, String> getTagNameMap();

	    protected Directory()
	    {}

	// VARIOUS METHODS

	    /**
	     * Gets a value indicating whether the directory is empty, meaning it contains no errors and no tag values.
	     */
	    public boolean isEmpty()
	    {
	        return _errorList.isEmpty() && _definedTagList.isEmpty();
	    }

	    /**
	     * Indicates whether the specified tag type has been set.
	     *
	     * @param tagType the tag type to check for
	     * @return true if a value exists for the specified tag type, false if not
	     */
	    @java.lang.SuppressWarnings({ "UnnecessaryBoxing" })
	    public boolean containsTag(int tagType)
	    {
	        return _tagMap.containsKey(Integer.valueOf(tagType));
	    }

	    /**
	     * Returns an Iterator of Tag instances that have been set in this Directory.
	     *
	     * @return an Iterator of Tag instances
	     */
	    @NotNull
	    public Collection<Tag> getTags()
	    {
	        return Collections.unmodifiableCollection(_definedTagList);
	    }

	    /**
	     * Returns the number of tags set in this Directory.
	     *
	     * @return the number of tags set in this Directory
	     */
	    public int getTagCount()
	    {
	        return _definedTagList.size();
	    }

	    /**
	     * Sets the descriptor used to interpret tag values.
	     *
	     * @param descriptor the descriptor used to interpret tag values
	     */
	    @java.lang.SuppressWarnings({ "ConstantConditions" })
	    public void setDescriptor(@NotNull TagDescriptor<?> descriptor)
	    {
	        if (descriptor == null)
	            throw new NullPointerException("cannot set a null descriptor");
	        _descriptor = descriptor;
	    }

	    /**
	     * Registers an error message with this directory.
	     *
	     * @param message an error message.
	     */
	    public void addError(@NotNull String message)
	    {
	        _errorList.add(message);
	    }

	    /**
	     * Gets a value indicating whether this directory has any error messages.
	     *
	     * @return true if the directory contains errors, otherwise false
	     */
	    public boolean hasErrors()
	    {
	        return _errorList.size() > 0;
	    }

	    /**
	     * Used to iterate over any error messages contained in this directory.
	     *
	     * @return an iterable collection of error message strings.
	     */
	    @NotNull
	    public Iterable<String> getErrors()
	    {
	        return Collections.unmodifiableCollection(_errorList);
	    }

	    /** Returns the count of error messages in this directory. */
	    public int getErrorCount()
	    {
	        return _errorList.size();
	    }

	    @Nullable
	    public Directory getParent()
	    {
	        return _parent;
	    }

	    public void setParent(@NotNull Directory parent)
	    {
	        _parent = parent;
	    }

	// TAG SETTERS

	    /**
	     * Sets an <code>int</code> value for the specified tag.
	     *
	     * @param tagType the tag's value as an int
	     * @param value   the value for the specified tag as an int
	     */
	    public void setInt(int tagType, int value)
	    {
	        setObject(tagType, value);
	    }

	    /**
	     * Sets an <code>int[]</code> (array) for the specified tag.
	     *
	     * @param tagType the tag identifier
	     * @param ints    the int array to store
	     */
	    public void setIntArray(int tagType, @NotNull int[] ints)
	    {
	        setObjectArray(tagType, ints);
	    }

	    /**
	     * Sets a <code>float</code> value for the specified tag.
	     *
	     * @param tagType the tag's value as an int
	     * @param value   the value for the specified tag as a float
	     */
	    public void setFloat(int tagType, float value)
	    {
	        setObject(tagType, value);
	    }

	    /**
	     * Sets a <code>float[]</code> (array) for the specified tag.
	     *
	     * @param tagType the tag identifier
	     * @param floats  the float array to store
	     */
	    public void setFloatArray(int tagType, @NotNull float[] floats)
	    {
	        setObjectArray(tagType, floats);
	    }

	    /**
	     * Sets a <code>double</code> value for the specified tag.
	     *
	     * @param tagType the tag's value as an int
	     * @param value   the value for the specified tag as a double
	     */
	    public void setDouble(int tagType, double value)
	    {
	        setObject(tagType, value);
	    }

	    /**
	     * Sets a <code>double[]</code> (array) for the specified tag.
	     *
	     * @param tagType the tag identifier
	     * @param doubles the double array to store
	     */
	    public void setDoubleArray(int tagType, @NotNull double[] doubles)
	    {
	        setObjectArray(tagType, doubles);
	    }

	    /**
	     * Sets a <code>StringValue</code> value for the specified tag.
	     *
	     * @param tagType the tag's value as an int
	     * @param value   the value for the specified tag as a StringValue
	     */
	    @java.lang.SuppressWarnings({ "ConstantConditions" })
	    public void setStringValue(int tagType, @NotNull StringValue value)
	    {
	        if (value == null)
	            throw new NullPointerException("cannot set a null StringValue");
	        setObject(tagType, value);
	    }

	    /**
	     * Sets a <code>String</code> value for the specified tag.
	     *
	     * @param tagType the tag's value as an int
	     * @param value   the value for the specified tag as a String
	     */
	    @java.lang.SuppressWarnings({ "ConstantConditions" })
	    public void setString(int tagType, @NotNull String value)
	    {
	        if (value == null)
	            throw new NullPointerException("cannot set a null String");
	        setObject(tagType, value);
	    }

	    /**
	     * Sets a <code>String[]</code> (array) for the specified tag.
	     *
	     * @param tagType the tag identifier
	     * @param strings the String array to store
	     */
	    public void setStringArray(int tagType, @NotNull String[] strings)
	    {
	        setObjectArray(tagType, strings);
	    }

	    /**
	     * Sets a <code>StringValue[]</code> (array) for the specified tag.
	     *
	     * @param tagType the tag identifier
	     * @param strings the StringValue array to store
	     */
	    public void setStringValueArray(int tagType, @NotNull StringValue[] strings)
	    {
	        setObjectArray(tagType, strings);
	    }

	    /**
	     * Sets a <code>boolean</code> value for the specified tag.
	     *
	     * @param tagType the tag's value as an int
	     * @param value   the value for the specified tag as a boolean
	     */
	    public void setBoolean(int tagType, boolean value)
	    {
	        setObject(tagType, value);
	    }

	    /**
	     * Sets a <code>long</code> value for the specified tag.
	     *
	     * @param tagType the tag's value as an int
	     * @param value   the value for the specified tag as a long
	     */
	    public void setLong(int tagType, long value)
	    {
	        setObject(tagType, value);
	    }

	    /**
	     * Sets a <code>java.util.Date</code> value for the specified tag.
	     *
	     * @param tagType the tag's value as an int
	     * @param value   the value for the specified tag as a java.util.Date
	     */
	    public void setDate(int tagType, @NotNull java.util.Date value)
	    {
	        setObject(tagType, value);
	    }

	    /**
	     * Sets a <code>Rational</code> value for the specified tag.
	     *
	     * @param tagType  the tag's value as an int
	     * @param rational rational number
	     */
	    public void setRational(int tagType, @NotNull Rational rational)
	    {
	        setObject(tagType, rational);
	    }

	    /**
	     * Sets a <code>Rational[]</code> (array) for the specified tag.
	     *
	     * @param tagType   the tag identifier
	     * @param rationals the Rational array to store
	     */
	    public void setRationalArray(int tagType, @NotNull Rational[] rationals)
	    {
	        setObjectArray(tagType, rationals);
	    }

	    /**
	     * Sets a <code>byte[]</code> (array) for the specified tag.
	     *
	     * @param tagType the tag identifier
	     * @param bytes   the byte array to store
	     */
	    public void setByteArray(int tagType, @NotNull byte[] bytes)
	    {
	        setObjectArray(tagType, bytes);
	    }

	    /**
	     * Sets a <code>Object</code> for the specified tag.
	     *
	     * @param tagType the tag's value as an int
	     * @param value   the value for the specified tag
	     * @throws NullPointerException if value is <code>null</code>
	     */
	    @java.lang.SuppressWarnings( { "ConstantConditions", "UnnecessaryBoxing" })
	    public void setObject(int tagType, @NotNull Object value)
	    {
	        if (value == null)
	            throw new NullPointerException("cannot set a null object");

	        if (!_tagMap.containsKey(Integer.valueOf(tagType))) {
	            _definedTagList.add(new Tag(tagType, this));
	        }
//	        else {
//	            final Object oldValue = _tagMap.get(tagType);
//	            if (!oldValue.equals(value))
//	                addError(String.format("Overwritten tag 0x%s (%s).  Old=%s, New=%s", Integer.toHexString(tagType), getTagName(tagType), oldValue, value));
//	        }
	        _tagMap.put(tagType, value);
	    }

	    /**
	     * Sets an array <code>Object</code> for the specified tag.
	     *
	     * @param tagType the tag's value as an int
	     * @param array   the array of values for the specified tag
	     */
	    public void setObjectArray(int tagType, @NotNull Object array)
	    {
	        // for now, we don't do anything special -- this method might be a candidate for removal once the dust settles
	        setObject(tagType, array);
	    }

	// TAG GETTERS

	    /**
	     * Returns the specified tag's value as an int, if possible.  Every attempt to represent the tag's value as an int
	     * is taken.  Here is a list of the action taken depending upon the tag's original type:
	     * <ul>
	     * <li> int - Return unchanged.
	     * <li> Number - Return an int value (real numbers are truncated).
	     * <li> Rational - Truncate any fractional part and returns remaining int.
	     * <li> String - Attempt to parse string as an int.  If this fails, convert the char[] to an int (using shifts and OR).
	     * <li> Rational[] - Return int value of first item in array.
	     * <li> byte[] - Return int value of first item in array.
	     * <li> int[] - Return int value of first item in array.
	     * </ul>
	     *
	     * @throws MetadataException if no value exists for tagType or if it cannot be converted to an int.
	     */
	    public int getInt(int tagType) throws MetadataException
	    {
	        Integer integer = getInteger(tagType);
	        if (integer!=null)
	            return integer;

	        Object o = getObject(tagType);
	        if (o == null)
	            throw new MetadataException("Tag '" + getTagName(tagType) + "' has not been set -- check using containsTag() first");
	        throw new MetadataException("Tag '" + tagType + "' cannot be converted to int.  It is of type '" + o.getClass() + "'.");
	    }

	    /**
	     * Returns the specified tag's value as an Integer, if possible.  Every attempt to represent the tag's value as an
	     * Integer is taken.  Here is a list of the action taken depending upon the tag's original type:
	     * <ul>
	     * <li> int - Return unchanged
	     * <li> Number - Return an int value (real numbers are truncated)
	     * <li> Rational - Truncate any fractional part and returns remaining int
	     * <li> String - Attempt to parse string as an int.  If this fails, convert the char[] to an int (using shifts and OR)
	     * <li> Rational[] - Return int value of first item in array if length &gt; 0
	     * <li> byte[] - Return int value of first item in array if length &gt; 0
	     * <li> int[] - Return int value of first item in array if length &gt; 0
	     * </ul>
	     *
	     * If the value is not found or cannot be converted to int, <code>null</code> is returned.
	     */
	    @Nullable
	    public Integer getInteger(int tagType)
	    {
	        Object o = getObject(tagType);

	        if (o == null)
	            return null;

	        if (o instanceof Number) {
	            return ((Number)o).intValue();
	        } else if (o instanceof String || o instanceof StringValue) {
	            try {
	                return Integer.parseInt(o.toString());
	            } catch (NumberFormatException nfe) {
	                // convert the char array to an int
	                String s = o.toString();
	                byte[] bytes = s.getBytes();
	                long val = 0;
	                for (byte aByte : bytes) {
	                    val = val << 8;
	                    val += (aByte & 0xff);
	                }
	                return (int)val;
	            }
	        } else if (o instanceof Rational[]) {
	            Rational[] rationals = (Rational[])o;
	            if (rationals.length == 1)
	                return rationals[0].intValue();
	        } else if (o instanceof byte[]) {
	            byte[] bytes = (byte[])o;
	            if (bytes.length == 1)
	                return (int)bytes[0];
	        } else if (o instanceof int[]) {
	            int[] ints = (int[])o;
	            if (ints.length == 1)
	                return ints[0];
	        } else if (o instanceof short[]) {
	            short[] shorts = (short[])o;
	            if (shorts.length == 1)
	                return (int)shorts[0];
	        }
	        return null;
	    }

	    /**
	     * Gets the specified tag's value as a String array, if possible.  Only supported
	     * where the tag is set as StringValue[], String[], StringValue, String, int[], byte[] or Rational[].
	     *
	     * @param tagType the tag identifier
	     * @return the tag's value as an array of Strings. If the value is unset or cannot be converted, <code>null</code> is returned.
	     */
	    @Nullable
	    public String[] getStringArray(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o == null)
	            return null;
	        if (o instanceof String[])
	            return (String[])o;
	        if (o instanceof String)
	            return new String[] { (String)o };
	        if (o instanceof StringValue)
	            return new String[] { o.toString() };
	        if (o instanceof StringValue[]) {
	            StringValue[] stringValues = (StringValue[])o;
	            String[] strings = new String[stringValues.length];
	            for (int i = 0; i < strings.length; i++)
	                strings[i] = stringValues[i].toString();
	            return strings;
	        }
	        if (o instanceof int[]) {
	            int[] ints = (int[])o;
	            String[] strings = new String[ints.length];
	            for (int i = 0; i < strings.length; i++)
	                strings[i] = Integer.toString(ints[i]);
	            return strings;
	        }
	        if (o instanceof byte[]) {
	            byte[] bytes = (byte[])o;
	            String[] strings = new String[bytes.length];
	            for (int i = 0; i < strings.length; i++)
	                strings[i] = Byte.toString(bytes[i]);
	            return strings;
	        }
	        if (o instanceof Rational[]) {
	            Rational[] rationals = (Rational[])o;
	            String[] strings = new String[rationals.length];
	            for (int i = 0; i < strings.length; i++)
	                strings[i] = rationals[i].toSimpleString(false);
	            return strings;
	        }
	        return null;
	    }

	    /**
	     * Gets the specified tag's value as a StringValue array, if possible.
	     * Only succeeds if the tag is set as StringValue[], or StringValue.
	     *
	     * @param tagType the tag identifier
	     * @return the tag's value as an array of StringValues. If the value is unset or cannot be converted, <code>null</code> is returned.
	     */
	    @Nullable
	    public StringValue[] getStringValueArray(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o == null)
	            return null;
	        if (o instanceof StringValue[])
	            return (StringValue[])o;
	        if (o instanceof StringValue)
	            return new StringValue[] {(StringValue) o};
	        return null;
	    }

	    /**
	     * Gets the specified tag's value as an int array, if possible.  Only supported
	     * where the tag is set as String, Integer, int[], byte[] or Rational[].
	     *
	     * @param tagType the tag identifier
	     * @return the tag's value as an int array
	     */
	    @Nullable
	    public int[] getIntArray(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o == null)
	            return null;
	        if (o instanceof int[])
	            return (int[])o;
	        if (o instanceof Rational[]) {
	            Rational[] rationals = (Rational[])o;
	            int[] ints = new int[rationals.length];
	            for (int i = 0; i < ints.length; i++) {
	                ints[i] = rationals[i].intValue();
	            }
	            return ints;
	        }
	        if (o instanceof short[]) {
	            short[] shorts = (short[])o;
	            int[] ints = new int[shorts.length];
	            for (int i = 0; i < shorts.length; i++) {
	                ints[i] = shorts[i];
	            }
	            return ints;
	        }
	        if (o instanceof byte[]) {
	            byte[] bytes = (byte[])o;
	            int[] ints = new int[bytes.length];
	            for (int i = 0; i < bytes.length; i++) {
	                ints[i] = bytes[i];
	            }
	            return ints;
	        }
	        if (o instanceof CharSequence) {
	            CharSequence str = (CharSequence)o;
	            int[] ints = new int[str.length()];
	            for (int i = 0; i < str.length(); i++) {
	                ints[i] = str.charAt(i);
	            }
	            return ints;
	        }
	        if (o instanceof Integer)
	            return new int[] { (Integer)o };

	        return null;
	    }

	    /**
	     * Gets the specified tag's value as an byte array, if possible.  Only supported
	     * where the tag is set as String, Integer, int[], byte[] or Rational[].
	     *
	     * @param tagType the tag identifier
	     * @return the tag's value as a byte array
	     */
	    @Nullable
	    public byte[] getByteArray(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o == null) {
	            return null;
	        } else if (o instanceof StringValue) {
	            return ((StringValue)o).getBytes();
	        } else if (o instanceof Rational[]) {
	            Rational[] rationals = (Rational[])o;
	            byte[] bytes = new byte[rationals.length];
	            for (int i = 0; i < bytes.length; i++) {
	                bytes[i] = rationals[i].byteValue();
	            }
	            return bytes;
	        } else if (o instanceof byte[]) {
	            return (byte[])o;
	        } else if (o instanceof int[]) {
	            int[] ints = (int[])o;
	            byte[] bytes = new byte[ints.length];
	            for (int i = 0; i < ints.length; i++) {
	                bytes[i] = (byte)ints[i];
	            }
	            return bytes;
	        } else if (o instanceof short[]) {
	            short[] shorts = (short[])o;
	            byte[] bytes = new byte[shorts.length];
	            for (int i = 0; i < shorts.length; i++) {
	                bytes[i] = (byte)shorts[i];
	            }
	            return bytes;
	        } else if (o instanceof CharSequence) {
	            CharSequence str = (CharSequence)o;
	            byte[] bytes = new byte[str.length()];
	            for (int i = 0; i < str.length(); i++) {
	                bytes[i] = (byte)str.charAt(i);
	            }
	            return bytes;
	        }
	        if (o instanceof Integer)
	            return new byte[] { ((Integer)o).byteValue() };

	        return null;
	    }

	    /** Returns the specified tag's value as a double, if possible. */
	    public double getDouble(int tagType) throws MetadataException
	    {
	        Double value = getDoubleObject(tagType);
	        if (value!=null)
	            return value;
	        Object o = getObject(tagType);
	        if (o == null)
	            throw new MetadataException("Tag '" + getTagName(tagType) + "' has not been set -- check using containsTag() first");
	        throw new MetadataException("Tag '" + tagType + "' cannot be converted to a double.  It is of type '" + o.getClass() + "'.");
	    }
	    /** Returns the specified tag's value as a Double.  If the tag is not set or cannot be converted, <code>null</code> is returned. */
	    @Nullable
	    public Double getDoubleObject(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o == null)
	            return null;
	        if (o instanceof String || o instanceof StringValue) {
	            try {
	                return Double.parseDouble(o.toString());
	            } catch (NumberFormatException nfe) {
	                return null;
	            }
	        }
	        if (o instanceof Number)
	            return ((Number)o).doubleValue();

	        return null;
	    }

	    /** Returns the specified tag's value as a float, if possible. */
	    public float getFloat(int tagType) throws MetadataException
	    {
	        Float value = getFloatObject(tagType);
	        if (value!=null)
	            return value;
	        Object o = getObject(tagType);
	        if (o == null)
	            throw new MetadataException("Tag '" + getTagName(tagType) + "' has not been set -- check using containsTag() first");
	        throw new MetadataException("Tag '" + tagType + "' cannot be converted to a float.  It is of type '" + o.getClass() + "'.");
	    }

	    /** Returns the specified tag's value as a float.  If the tag is not set or cannot be converted, <code>null</code> is returned. */
	    @Nullable
	    public Float getFloatObject(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o == null)
	            return null;
	        if (o instanceof String || o instanceof StringValue) {
	            try {
	                return Float.parseFloat(o.toString());
	            } catch (NumberFormatException nfe) {
	                return null;
	            }
	        }
	        if (o instanceof Number)
	            return ((Number)o).floatValue();
	        return null;
	    }

	    /** Returns the specified tag's value as a long, if possible. */
	    public long getLong(int tagType) throws MetadataException
	    {
	        Long value = getLongObject(tagType);
	        if (value != null)
	            return value;
	        Object o = getObject(tagType);
	        if (o == null)
	            throw new MetadataException("Tag '" + getTagName(tagType) + "' has not been set -- check using containsTag() first");
	        throw new MetadataException("Tag '" + tagType + "' cannot be converted to a long.  It is of type '" + o.getClass() + "'.");
	    }

	    /** Returns the specified tag's value as a long.  If the tag is not set or cannot be converted, <code>null</code> is returned. */
	    @Nullable
	    public Long getLongObject(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o == null)
	            return null;
	        if (o instanceof Number)
	            return ((Number)o).longValue();
	        if (o instanceof String || o instanceof StringValue) {
	            try {
	                return Long.parseLong(o.toString());
	            } catch (NumberFormatException nfe) {
	                return null;
	            }
	        } else if (o instanceof Rational[]) {
	            Rational[] rationals = (Rational[])o;
	            if (rationals.length == 1)
	                return rationals[0].longValue();
	        } else if (o instanceof byte[]) {
	            byte[] bytes = (byte[])o;
	            if (bytes.length == 1)
	                return (long)bytes[0];
	        } else if (o instanceof int[]) {
	            int[] ints = (int[])o;
	            if (ints.length == 1)
	                return (long)ints[0];
	        } else if (o instanceof short[]) {
	            short[] shorts = (short[])o;
	            if (shorts.length == 1)
	                return (long)shorts[0];
	        }
	        return null;
	    }

	    /** Returns the specified tag's value as a boolean, if possible. */
	    public boolean getBoolean(int tagType) throws MetadataException
	    {
	        Boolean value = getBooleanObject(tagType);
	        if (value != null)
	            return value;
	        Object o = getObject(tagType);
	        if (o == null)
	            throw new MetadataException("Tag '" + getTagName(tagType) + "' has not been set -- check using containsTag() first");
	        throw new MetadataException("Tag '" + tagType + "' cannot be converted to a boolean.  It is of type '" + o.getClass() + "'.");
	    }

	    /** Returns the specified tag's value as a boolean.  If the tag is not set or cannot be converted, <code>null</code> is returned. */
	    @Nullable
	    @SuppressWarnings(value = "NP_BOOLEAN_RETURN_NULL", justification = "keep API interface consistent")
	    public Boolean getBooleanObject(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o == null)
	            return null;
	        if (o instanceof Boolean)
	            return (Boolean)o;
	        if (o instanceof String || o instanceof StringValue) {
	            try {
	                return Boolean.getBoolean(o.toString());
	            } catch (NumberFormatException nfe) {
	                return null;
	            }
	        }
	        if (o instanceof Number)
	            return (((Number)o).doubleValue() != 0);
	        return null;
	    }

	    /**
	     * Returns the specified tag's value as a java.util.Date.  If the value is unset or cannot be converted, <code>null</code> is returned.
	     * <p>
	     * If the underlying value is a {@link String}, then attempts will be made to parse the string as though it is in
	     * the GMT {@link TimeZone}.  If the {@link TimeZone} is known, call the overload that accepts one as an argument.
	     */
	    @Nullable
	    public java.util.Date getDate(int tagType)
	    {
	        return getDate(tagType, null, null);
	    }

	    /**
	     * Returns the specified tag's value as a java.util.Date.  If the value is unset or cannot be converted, <code>null</code> is returned.
	     * <p>
	     * If the underlying value is a {@link String}, then attempts will be made to parse the string as though it is in
	     * the {@link TimeZone} represented by the {@code timeZone} parameter (if it is non-null).  Note that this parameter
	     * is only considered if the underlying value is a string and it has no time zone information, otherwise it has no effect.
	     */
	    @Nullable
	    public java.util.Date getDate(int tagType, @Nullable TimeZone timeZone)
	    {
	        return getDate(tagType, null, timeZone);
	    }

	    /**
	     * Returns the specified tag's value as a java.util.Date.  If the value is unset or cannot be converted, <code>null</code> is returned.
	     * <p>
	     * If the underlying value is a {@link String}, then attempts will be made to parse the string as though it is in
	     * the {@link TimeZone} represented by the {@code timeZone} parameter (if it is non-null).  Note that this parameter
	     * is only considered if the underlying value is a string and it has no time zone information, otherwise it has no effect.
	     * In addition, the {@code subsecond} parameter, which specifies the number of digits after the decimal point in the seconds,
	     * is set to the returned Date. This parameter is only considered if the underlying value is a string and is has
	     * no subsecond information, otherwise it has no effect.
	     *
	     * @param tagType the tag identifier
	     * @param subsecond the subsecond value for the Date
	     * @param timeZone the time zone to use
	     * @return a Date representing the time value
	     */
	    @Nullable
	    public java.util.Date getDate(int tagType, @Nullable String subsecond, @Nullable TimeZone timeZone)
	    {
	        Object o = getObject(tagType);

	        if (o instanceof java.util.Date)
	            return (java.util.Date)o;

	        java.util.Date date = null;

	        if ((o instanceof String) || (o instanceof StringValue)) {
	            // This seems to cover all known Exif and Xmp date strings
	            // Note that "    :  :     :  :  " is a valid date string according to the Exif spec (which means 'unknown date'): http://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif/datetimeoriginal.html
	            String datePatterns[] = {
	                    "yyyy:MM:dd HH:mm:ss",
	                    "yyyy:MM:dd HH:mm",
	                    "yyyy-MM-dd HH:mm:ss",
	                    "yyyy-MM-dd HH:mm",
	                    "yyyy.MM.dd HH:mm:ss",
	                    "yyyy.MM.dd HH:mm",
	                    "yyyy-MM-dd'T'HH:mm:ss",
	                    "yyyy-MM-dd'T'HH:mm",
	                    "yyyy-MM-dd",
	                    "yyyy-MM",
	                    "yyyyMMdd", // as used in IPTC data
	                    "yyyy" };

	            String dateString = o.toString();

	            // if the date string has subsecond information, it supersedes the subsecond parameter
	            Pattern subsecondPattern = Pattern.compile("(\\d\\d:\\d\\d:\\d\\d)(\\.\\d+)");
	            Matcher subsecondMatcher = subsecondPattern.matcher(dateString);
	            if (subsecondMatcher.find()) {
	                subsecond = subsecondMatcher.group(2).substring(1);
	                dateString = subsecondMatcher.replaceAll("$1");
	            }

	            // if the date string has time zone information, it supersedes the timeZone parameter
	            Pattern timeZonePattern = Pattern.compile("(Z|[+-]\\d\\d:\\d\\d|[+-]\\d\\d\\d\\d)$");
	            Matcher timeZoneMatcher = timeZonePattern.matcher(dateString);
	            if (timeZoneMatcher.find()) {
	                timeZone = TimeZone.getTimeZone("GMT" + timeZoneMatcher.group().replaceAll("Z", ""));
	                dateString = timeZoneMatcher.replaceAll("");
	            }

	            for (String datePattern : datePatterns) {
	                try {
	                    DateFormat parser = new SimpleDateFormat(datePattern);
	                    if (timeZone != null)
	                        parser.setTimeZone(timeZone);
	                    else
	                        parser.setTimeZone(TimeZone.getTimeZone("GMT")); // don't interpret zone time

	                    date = parser.parse(dateString);
	                    break;
	                } catch (ParseException ex) {
	                    // simply try the next pattern
	                }
	            }
	        }

	        if (date == null)
	            return null;

	        if (subsecond == null)
	            return date;

	        try {
	            int millisecond = (int) (Double.parseDouble("." + subsecond) * 1000);
	            if (millisecond >= 0 && millisecond < 1000) {
	                Calendar calendar = Calendar.getInstance();
	                calendar.setTime(date);
	                calendar.set(Calendar.MILLISECOND, millisecond);
	                return calendar.getTime();
	            }
	            return date;
	        } catch (NumberFormatException e) {
	            return date;
	        }
	    }

	    /** Returns the specified tag's value as a Rational.  If the value is unset or cannot be converted, <code>null</code> is returned. */
	    @Nullable
	    public Rational getRational(int tagType)
	    {
	        Object o = getObject(tagType);

	        if (o == null)
	            return null;

	        if (o instanceof Rational)
	            return (Rational)o;
	        if (o instanceof Integer)
	            return new Rational((Integer)o, 1);
	        if (o instanceof Long)
	            return new Rational((Long)o, 1);

	        // NOTE not doing conversions for real number types

	        return null;
	    }

	    /** Returns the specified tag's value as an array of Rational.  If the value is unset or cannot be converted, <code>null</code> is returned. */
	    @Nullable
	    public Rational[] getRationalArray(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o == null)
	            return null;

	        if (o instanceof Rational[])
	            return (Rational[])o;

	        return null;
	    }

	    /**
	     * Returns the specified tag's value as a String.  This value is the 'raw' value.  A more presentable decoding
	     * of this value may be obtained from the corresponding Descriptor.
	     *
	     * @return the String representation of the tag's value, or
	     *         <code>null</code> if the tag hasn't been defined.
	     */
	    @Nullable
	    public String getString(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o == null)
	            return null;

	        if (o instanceof Rational)
	            return ((Rational)o).toSimpleString(true);

	        if (o.getClass().isArray()) {
	            // handle arrays of objects and primitives
	            int arrayLength = Array.getLength(o);
	            final Class<?> componentType = o.getClass().getComponentType();

	            StringBuilder string = new StringBuilder();

	            if (Object.class.isAssignableFrom(componentType)) {
	                // object array
	                for (int i = 0; i < arrayLength; i++) {
	                    if (i != 0)
	                        string.append(' ');
	                    string.append(Array.get(o, i).toString());
	                }
	            } else if (componentType.getName().equals("int")) {
	                for (int i = 0; i < arrayLength; i++) {
	                    if (i != 0)
	                        string.append(' ');
	                    string.append(Array.getInt(o, i));
	                }
	            } else if (componentType.getName().equals("short")) {
	                for (int i = 0; i < arrayLength; i++) {
	                    if (i != 0)
	                        string.append(' ');
	                    string.append(Array.getShort(o, i));
	                }
	            } else if (componentType.getName().equals("long")) {
	                for (int i = 0; i < arrayLength; i++) {
	                    if (i != 0)
	                        string.append(' ');
	                    string.append(Array.getLong(o, i));
	                }
	            } else if (componentType.getName().equals("float")) {
	                DecimalFormat format = new DecimalFormat(_floatFormatPattern);
	                for (int i = 0; i < arrayLength; i++) {
	                    if (i != 0)
	                        string.append(' ');
	                    String s = format.format(Array.getFloat(o, i));
	                    string.append(s.equals("-0") ? "0" : s);
	                }
	            } else if (componentType.getName().equals("double")) {
	                DecimalFormat format = new DecimalFormat(_floatFormatPattern);
	                for (int i = 0; i < arrayLength; i++) {
	                    if (i != 0)
	                        string.append(' ');
	                    String s = format.format(Array.getDouble(o, i));
	                    string.append(s.equals("-0") ? "0" : s);
	                }
	            } else if (componentType.getName().equals("byte")) {
	                for (int i = 0; i < arrayLength; i++) {
	                    if (i != 0)
	                        string.append(' ');
	                    string.append(Array.getByte(o, i) & 0xff);
	                }
	            } else {
	                addError("Unexpected array component type: " + componentType.getName());
	            }

	            return string.toString();
	        }

	        if (o instanceof Double)
	            return new DecimalFormat(_floatFormatPattern).format(((Double)o).doubleValue());

	        if (o instanceof Float)
	            return new DecimalFormat(_floatFormatPattern).format(((Float)o).floatValue());

	        // Note that several cameras leave trailing spaces (Olympus, Nikon) but this library is intended to show
	        // the actual data within the file.  It is not inconceivable that whitespace may be significant here, so we
	        // do not trim.  Also, if support is added for writing data back to files, this may cause issues.
	        // We leave trimming to the presentation layer.
	        return o.toString();
	    }

	    @Nullable
	    public String getString(int tagType, String charset)
	    {
	        byte[] bytes = getByteArray(tagType);
	        if (bytes==null)
	            return null;
	        try {
	            return new String(bytes, charset);
	        } catch (UnsupportedEncodingException e) {
	            return null;
	        }
	    }

	    @Nullable
	    public StringValue getStringValue(int tagType)
	    {
	        Object o = getObject(tagType);
	        if (o instanceof StringValue)
	            return (StringValue)o;
	        return null;
	    }

	    /**
	     * Returns the object hashed for the particular tag type specified, if available.
	     *
	     * @param tagType the tag type identifier
	     * @return the tag's value as an Object if available, else <code>null</code>
	     */
	    @java.lang.SuppressWarnings({ "UnnecessaryBoxing" })
	    @Nullable
	    public Object getObject(int tagType)
	    {
	        return _tagMap.get(Integer.valueOf(tagType));
	    }

	// OTHER METHODS

	    /**
	     * Returns the name of a specified tag as a String.
	     *
	     * @param tagType the tag type identifier
	     * @return the tag's name as a String
	     */
	    @NotNull
	    public String getTagName(int tagType)
	    {
	        HashMap<Integer, String> nameMap = getTagNameMap();
	        if (!nameMap.containsKey(tagType)) {
	            String hex = Integer.toHexString(tagType);
	            while (hex.length() < 4) {
	                hex = "0" + hex;
	            }
	            return "Unknown tag (0x" + hex + ")";
	        }
	        return nameMap.get(tagType);
	    }

	    /**
	     * Gets whether the specified tag is known by the directory and has a name.
	     *
	     * @param tagType the tag type identifier
	     * @return whether this directory has a name for the specified tag
	     */
	    public boolean hasTagName(int tagType)
	    {
	        return getTagNameMap().containsKey(tagType);
	    }

	    /**
	     * Provides a description of a tag's value using the descriptor set by
	     * <code>setDescriptor(Descriptor)</code>.
	     *
	     * @param tagType the tag type identifier
	     * @return the tag value's description as a String
	     */
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        assert(_descriptor != null);
	        return _descriptor.getDescription(tagType);
	    }

	    @Override
	    public String toString()
	    {
	        return String.format("%s Directory (%d %s)",
	            getName(),
	            _tagMap.size(),
	            _tagMap.size() == 1
	                ? "tag"
	                : "tags");
	    }
	}



	/**
	 * A top-level object that holds the metadata values extracted from an image.
	 * <p>
	 * Metadata objects may contain zero or more {@link Directory} objects.  Each directory may contain zero or more tags
	 * with corresponding values.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public final class Metadata
	{
	    /**
	     * The list of {@link Directory} instances in this container, in the order they were added.
	     */
	    @NotNull
	    private final List<Directory> _directories = new ArrayList<Directory>();

	    /**
	     * Returns an iterable set of the {@link Directory} instances contained in this metadata collection.
	     *
	     * @return an iterable set of directories
	     */
	    @NotNull
	    public Iterable<Directory> getDirectories()
	    {
	        return _directories;
	    }

	    @NotNull
	    //@SuppressWarnings("unchecked")
	    public <T extends Directory> Collection<T> getDirectoriesOfType(Class<T> type)
	    {
	        List<T> directories = new ArrayList<T>();
	        for (Directory dir : _directories) {
	            if (type.isAssignableFrom(dir.getClass())) {
	                directories.add((T)dir);
	            }
	        }
	        return directories;
	    }

	    /**
	     * Returns the count of directories in this metadata collection.
	     *
	     * @return the number of unique directory types set for this metadata collection
	     */
	    public int getDirectoryCount()
	    {
	        return _directories.size();
	    }

	    /**
	     * Adds a directory to this metadata collection.
	     *
	     * @param directory the {@link Directory} to add into this metadata collection.
	     */
	    public <T extends Directory> void addDirectory(@NotNull T directory)
	    {
	        if (directory == null) {
	            throw new IllegalArgumentException("Directory may not be null.");
	        }

	        _directories.add(directory);
	    }

	    /**
	     * Gets the first {@link Directory} of the specified type contained within this metadata collection.
	     * If no instances of this type are present, <code>null</code> is returned.
	     *
	     * @param type the Directory type
	     * @param <T> the Directory type
	     * @return the first Directory of type T in this metadata collection, or <code>null</code> if none exist
	     */
	    @Nullable
	    //@SuppressWarnings("unchecked")
	    public <T extends Directory> T getFirstDirectoryOfType(@NotNull Class<T> type)
	    {
	        for (Directory dir : _directories) {
	            if (type.isAssignableFrom(dir.getClass()))
	                return (T)dir;
	        }
	        return null;
	    }

	    /**
	     * Indicates whether an instance of the given directory type exists in this Metadata instance.
	     *
	     * @param type the {@link Directory} type
	     * @return <code>true</code> if a {@link Directory} of the specified type exists, otherwise <code>false</code>
	     */
	    public boolean containsDirectoryOfType(Class<? extends Directory> type)
	    {
	        for (Directory dir : _directories) {
	            if (type.isAssignableFrom(dir.getClass()))
	                return true;
	        }
	        return false;
	    }

	    /**
	     * Indicates whether any errors were reported during the reading of metadata values.
	     * This value will be true if Directory.hasErrors() is true for one of the contained {@link Directory} objects.
	     *
	     * @return whether one of the contained directories has an error
	     */
	    public boolean hasErrors()
	    {
	        for (Directory directory : getDirectories()) {
	            if (directory.hasErrors())
	                return true;
	        }
	        return false;
	    }

	    @Override
	    public String toString()
	    {
	        int count = getDirectoryCount();
	        return String.format("Metadata (%d %s)",
	            count,
	            count == 1
	                ? "directory"
	                : "directories");
	    }
	}
	
	/**
	 * An enumeration of the known segment types found in JPEG files.
	 *
	 * <ul>
	 *     <li>http://www.ozhiker.com/electronics/pjmt/jpeg_info/app_segments.html</li>
	 *     <li>http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/JPEG.html</li>
	 * </ul>
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public enum JpegSegmentType
	{
	    /** APP0 JPEG segment identifier. Commonly contains JFIF, JFXX. */
	    APP0((byte)0xE0, true),

	    /** APP1 JPEG segment identifier. Commonly contains Exif. XMP data is also kept in here, though usually in a second instance. */
	    APP1((byte)0xE1, true),

	        /** APP2 JPEG segment identifier. Commonly contains ICC. */
	    APP2((byte)0xE2, true),

	    /** APP3 JPEG segment identifier. */
	    APP3((byte)0xE3, true),

	    /** APP4 JPEG segment identifier. */
	    APP4((byte)0xE4, true),

	    /** APP5 JPEG segment identifier. */
	    APP5((byte)0xE5, true),

	    /** APP6 JPEG segment identifier. */
	    APP6((byte)0xE6, true),

	    /** APP7 JPEG segment identifier. */
	    APP7((byte)0xE7, true),

	    /** APP8 JPEG segment identifier. */
	    APP8((byte)0xE8, true),

	    /** APP9 JPEG segment identifier. */
	    APP9((byte)0xE9, true),

	    /** APPA (App10) JPEG segment identifier. Can contain Unicode comments, though {@link JpegSegmentType#COM} is more commonly used for comments. */
	    APPA((byte)0xEA, true),

	    /** APPB (App11) JPEG segment identifier. */
	    APPB((byte)0xEB, true),

	    /** APPC (App12) JPEG segment identifier. */
	    APPC((byte)0xEC, true),

	    /** APPD (App13) JPEG segment identifier. Commonly contains IPTC, Photoshop data. */
	    APPD((byte)0xED, true),

	    /** APPE (App14) JPEG segment identifier. Commonly contains Adobe data. */
	    APPE((byte)0xEE, true),

	    /** APPF (App15) JPEG segment identifier. */
	    APPF((byte)0xEF, true),

	    /** Start Of Image segment identifier. */
	    SOI((byte)0xD8, false),

	    /** Define Quantization Table segment identifier. */
	    DQT((byte)0xDB, false),

	    /** Define Number of Lines segment identifier. */
	    DNL((byte)0xDC, false),

	    /** Define Restart Interval segment identifier. */
	    DRI((byte)0xDD, false),

	    /** Define Hierarchical Progression segment identifier. */
	    DHP((byte)0xDE, false),

	    /** EXPand reference component(s) segment identifier. */
	    EXP((byte)0xDF, false),

	    /** Define Huffman Table segment identifier. */
	    DHT((byte)0xC4, false),

	    /** Define Arithmetic Coding conditioning segment identifier. */
	    DAC((byte)0xCC, false),

	    /** Start-of-Frame (0) segment identifier for Baseline DCT. */
	    SOF0((byte)0xC0, true),

	    /** Start-of-Frame (1) segment identifier for Extended sequential DCT. */
	    SOF1((byte)0xC1, true),

	    /** Start-of-Frame (2) segment identifier for Progressive DCT. */
	    SOF2((byte)0xC2, true),

	    /** Start-of-Frame (3) segment identifier for Lossless (sequential). */
	    SOF3((byte)0xC3, true),

//	    /** Start-of-Frame (4) segment identifier. */
//	    SOF4((byte)0xC4, true),

	    /** Start-of-Frame (5) segment identifier for Differential sequential DCT. */
	    SOF5((byte)0xC5, true),

	    /** Start-of-Frame (6) segment identifier for Differential progressive DCT. */
	    SOF6((byte)0xC6, true),

	    /** Start-of-Frame (7) segment identifier for Differential lossless (sequential). */
	    SOF7((byte)0xC7, true),

	    /** Reserved for JPEG extensions. */
	    JPG((byte)0xC8, true),

	    /** Start-of-Frame (9) segment identifier for Extended sequential DCT. */
	    SOF9((byte)0xC9, true),

	    /** Start-of-Frame (10) segment identifier for Progressive DCT. */
	    SOF10((byte)0xCA, true),

	    /** Start-of-Frame (11) segment identifier for Lossless (sequential). */
	    SOF11((byte)0xCB, true),

//	    /** Start-of-Frame (12) segment identifier. */
//	    SOF12((byte)0xCC, true),

	    /** Start-of-Frame (13) segment identifier for Differential sequential DCT. */
	    SOF13((byte)0xCD, true),

	    /** Start-of-Frame (14) segment identifier for Differential progressive DCT. */
	    SOF14((byte)0xCE, true),

	    /** Start-of-Frame (15) segment identifier for Differential lossless (sequential). */
	    SOF15((byte)0xCF, true),

	    /** JPEG comment segment identifier for comments. */
	    COM((byte)0xFE, true);

	    public static final Collection<JpegSegmentType> canContainMetadataTypes;

	    static {
	        List<JpegSegmentType> segmentTypes = new ArrayList<JpegSegmentType>();
	        for (JpegSegmentType segmentType : JpegSegmentType.class.getEnumConstants()) {
	            if (segmentType.canContainMetadata) {
	                segmentTypes.add(segmentType);
	            }
	        }
	        canContainMetadataTypes = segmentTypes;
	    }

	    public final byte byteValue;
	    public final boolean canContainMetadata;

	    JpegSegmentType(byte byteValue, boolean canContainMetadata)
	    {
	        this.byteValue = byteValue;
	        this.canContainMetadata = canContainMetadata;
	    }

	    @Nullable
	    public static JpegSegmentType fromByte(byte segmentTypeByte)
	    {
	        for (JpegSegmentType segmentType : JpegSegmentType.class.getEnumConstants()) {
	            if (segmentType.byteValue == segmentTypeByte)
	                return segmentType;
	        }
	        return null;
	    }
	}

	
	/**
	 * Defines an object that extracts metadata from in JPEG segments.
	 */
	public interface JpegSegmentMetadataReader
	{
	    /**
	     * Gets the set of JPEG segment types that this reader is interested in.
	     */
	    @NotNull
	    Iterable<JpegSegmentType> getSegmentTypes();

	    /**
	     * Extracts metadata from all instances of a particular JPEG segment type.
	     *
	     * @param segments A sequence of byte arrays from which the metadata should be extracted. These are in the order
	     *                 encountered in the original file.
	     * @param metadata The {@link Metadata} object into which extracted values should be merged.
	     * @param segmentType The {@link JpegSegmentType} being read.
	     */
	    void readJpegSegments(@NotNull final Iterable<byte[]> segments, @NotNull final Metadata metadata, @NotNull final JpegSegmentType segmentType);
	}
	
	/**
	 * Holds a collection of JPEG data segments.  This need not necessarily be all segments
	 * within the JPEG. For example, it may be convenient to store only the non-image
	 * segments when analysing metadata.
	 * <p>
	 * Segments are keyed via their {@link JpegSegmentType}. Where multiple segments use the
	 * same segment type, they will all be stored and available.
	 * <p>
	 * Each segment type may contain multiple entries. Conceptually the model is:
	 * <code>Map&lt;JpegSegmentType, Collection&lt;byte[]&gt;&gt;</code>. This class provides
	 * convenience methods around that structure.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegSegmentData
	{
	    // TODO key this on JpegSegmentType rather than Byte, and hopefully lose much of the use of 'byte' with this class
	    @NotNull
	    private final HashMap<Byte, List<byte[]>> _segmentDataMap = new HashMap<Byte, List<byte[]>>(10);

	    /**
	     * Adds segment bytes to the collection.
	     *
	     * @param segmentType  the type of the segment being added
	     * @param segmentBytes the byte array holding data for the segment being added
	     */
	    //@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
	    public void addSegment(byte segmentType, @NotNull byte[] segmentBytes)
	    {
	        getOrCreateSegmentList(segmentType).add(segmentBytes);
	    }

	    /**
	     * Gets the set of JPEG segment type identifiers.
	     */
	    public Iterable<JpegSegmentType> getSegmentTypes()
	    {
	        Set<JpegSegmentType> segmentTypes = new HashSet<JpegSegmentType>();

	        for (Byte segmentTypeByte : _segmentDataMap.keySet())
	        {
	            JpegSegmentType segmentType = JpegSegmentType.fromByte(segmentTypeByte);
	            if (segmentType == null) {
	                throw new IllegalStateException("Should not have a segmentTypeByte that is not in the enum: " + Integer.toHexString(segmentTypeByte));
	            }
	            segmentTypes.add(segmentType);
	        }

	        return segmentTypes;
	    }

	    /**
	     * Gets the first JPEG segment data for the specified type.
	     *
	     * @param segmentType the JpegSegmentType for the desired segment
	     * @return a byte[] containing segment data or null if no data exists for that segment
	     */
	    @Nullable
	    public byte[] getSegment(byte segmentType)
	    {
	        return getSegment(segmentType, 0);
	    }

	    /**
	     * Gets the first JPEG segment data for the specified type.
	     *
	     * @param segmentType the JpegSegmentType for the desired segment
	     * @return a byte[] containing segment data or null if no data exists for that segment
	     */
	    @Nullable
	    public byte[] getSegment(@NotNull JpegSegmentType segmentType)
	    {
	        return getSegment(segmentType.byteValue, 0);
	    }

	    /**
	     * Gets segment data for a specific occurrence and type.  Use this method when more than one occurrence
	     * of segment data for a given type exists.
	     *
	     * @param segmentType identifies the required segment
	     * @param occurrence  the zero-based index of the occurrence
	     * @return the segment data as a byte[], or null if no segment exists for the type &amp; occurrence
	     */
	    @Nullable
	    public byte[] getSegment(@NotNull JpegSegmentType segmentType, int occurrence)
	    {
	        return getSegment(segmentType.byteValue, occurrence);
	    }

	    /**
	     * Gets segment data for a specific occurrence and type.  Use this method when more than one occurrence
	     * of segment data for a given type exists.
	     *
	     * @param segmentType identifies the required segment
	     * @param occurrence  the zero-based index of the occurrence
	     * @return the segment data as a byte[], or null if no segment exists for the type &amp; occurrence
	     */
	    @Nullable
	    public byte[] getSegment(byte segmentType, int occurrence)
	    {
	        final List<byte[]> segmentList = getSegmentList(segmentType);

	        return segmentList != null && segmentList.size() > occurrence
	                ? segmentList.get(occurrence)
	                : null;
	    }

	    /**
	     * Returns all instances of a given JPEG segment.  If no instances exist, an empty sequence is returned.
	     *
	     * @param segmentType a number which identifies the type of JPEG segment being queried
	     * @return zero or more byte arrays, each holding the data of a JPEG segment
	     */
	    @NotNull
	    public Iterable<byte[]> getSegments(@NotNull JpegSegmentType segmentType)
	    {
	        return getSegments(segmentType.byteValue);
	    }

	    /**
	     * Returns all instances of a given JPEG segment.  If no instances exist, an empty sequence is returned.
	     *
	     * @param segmentType a number which identifies the type of JPEG segment being queried
	     * @return zero or more byte arrays, each holding the data of a JPEG segment
	     */
	    @NotNull
	    public Iterable<byte[]> getSegments(byte segmentType)
	    {
	        final List<byte[]> segmentList = getSegmentList(segmentType);
	        return segmentList == null ? new ArrayList<byte[]>() : segmentList;
	    }

	    @Nullable
	    private List<byte[]> getSegmentList(byte segmentType)
	    {
	        return _segmentDataMap.get(segmentType);
	    }

	    @NotNull
	    private List<byte[]> getOrCreateSegmentList(byte segmentType)
	    {
	        List<byte[]> segmentList;
	        if (_segmentDataMap.containsKey(segmentType)) {
	            segmentList = _segmentDataMap.get(segmentType);
	        } else {
	            segmentList = new ArrayList<byte[]>();
	            _segmentDataMap.put(segmentType, segmentList);
	        }
	        return segmentList;
	    }

	    /**
	     * Returns the count of segment data byte arrays stored for a given segment type.
	     *
	     * @param segmentType identifies the required segment
	     * @return the segment count (zero if no segments exist).
	     */
	    public int getSegmentCount(@NotNull JpegSegmentType segmentType)
	    {
	        return getSegmentCount(segmentType.byteValue);
	    }

	    /**
	     * Returns the count of segment data byte arrays stored for a given segment type.
	     *
	     * @param segmentType identifies the required segment
	     * @return the segment count (zero if no segments exist).
	     */
	    public int getSegmentCount(byte segmentType)
	    {
	        final List<byte[]> segmentList = getSegmentList(segmentType);
	        return segmentList == null ? 0 : segmentList.size();
	    }

	    /**
	     * Removes a specified instance of a segment's data from the collection.  Use this method when more than one
	     * occurrence of segment data exists for a given type exists.
	     *
	     * @param segmentType identifies the required segment
	     * @param occurrence  the zero-based index of the segment occurrence to remove.
	     */
	    //@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
	    public void removeSegmentOccurrence(@NotNull JpegSegmentType segmentType, int occurrence)
	    {
	        removeSegmentOccurrence(segmentType.byteValue, occurrence);
	    }

	    /**
	     * Removes a specified instance of a segment's data from the collection.  Use this method when more than one
	     * occurrence of segment data exists for a given type exists.
	     *
	     * @param segmentType identifies the required segment
	     * @param occurrence  the zero-based index of the segment occurrence to remove.
	     */
	    //@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
	    public void removeSegmentOccurrence(byte segmentType, int occurrence)
	    {
	        final List<byte[]> segmentList = _segmentDataMap.get(segmentType);
	        segmentList.remove(occurrence);
	    }

	    /**
	     * Removes all segments from the collection having the specified type.
	     *
	     * @param segmentType identifies the required segment
	     */
	    public void removeSegment(@NotNull JpegSegmentType segmentType)
	    {
	        removeSegment(segmentType.byteValue);
	    }

	    /**
	     * Removes all segments from the collection having the specified type.
	     *
	     * @param segmentType identifies the required segment
	     */
	    public void removeSegment(byte segmentType)
	    {
	        _segmentDataMap.remove(segmentType);
	    }

	    /**
	     * Determines whether data is present for a given segment type.
	     *
	     * @param segmentType identifies the required segment
	     * @return true if data exists, otherwise false
	     */
	    public boolean containsSegment(@NotNull JpegSegmentType segmentType)
	    {
	        return containsSegment(segmentType.byteValue);
	    }

	    /**
	     * Determines whether data is present for a given segment type.
	     *
	     * @param segmentType identifies the required segment
	     * @return true if data exists, otherwise false
	     */
	    public boolean containsSegment(byte segmentType)
	    {
	        return _segmentDataMap.containsKey(segmentType);
	    }
	}
	
	/**
	 * An exception class thrown upon an unexpected condition that was fatal for the processing of an image.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class ImageProcessingException extends CompoundException
	{
	    private static final long serialVersionUID = -9115669182209912676L;

	    public ImageProcessingException(@Nullable String message)
	    {
	        super(message);
	    }

	    public ImageProcessingException(@Nullable String message, @Nullable Throwable cause)
	    {
	        super(message, cause);
	    }

	    public ImageProcessingException(@Nullable Throwable cause)
	    {
	        super(cause);
	    }
	}

	/**
	 * An exception class thrown upon unexpected and fatal conditions while processing a JPEG file.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegProcessingException extends ImageProcessingException
	{
	    private static final long serialVersionUID = -7870179776125450158L;

	    public JpegProcessingException(@Nullable String message)
	    {
	        super(message);
	    }

	    public JpegProcessingException(@Nullable String message, @Nullable Throwable cause)
	    {
	        super(message, cause);
	    }

	    public JpegProcessingException(@Nullable Throwable cause)
	    {
	        super(cause);
	    }
	}

	
	/**
	 * Performs read functions of JPEG files, returning specific file segments.
	 * <p>
	 * JPEG files are composed of a sequence of consecutive JPEG 'segments'. Each is identified by one of a set of byte
	 * values, modelled in the {@link JpegSegmentType} enumeration. Use <code>readSegments</code> to read out the some
	 * or all segments into a {@link JpegSegmentData} object, from which the raw JPEG segment byte arrays may be accessed.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public static class JpegSegmentReader
	{
	    /**
	     * The 0xFF byte that signals the start of a segment.
	     */
	    private static final byte SEGMENT_IDENTIFIER = (byte) 0xFF;

	    /**
	     * Private, because this segment crashes my algorithm, and searching for it doesn't work (yet).
	     */
	    private static final byte SEGMENT_SOS = (byte) 0xDA;

	    /**
	     * Private, because one wouldn't search for it.
	     */
	    private static final byte MARKER_EOI = (byte) 0xD9;

	    /**
	     * Processes the provided JPEG data, and extracts the specified JPEG segments into a {@link JpegSegmentData} object.
	     * <p>
	     * Will not return SOS (start of scan) or EOI (end of image) segments.
	     *
	     * @param file a {@link File} from which the JPEG data will be read.
	     * @param segmentTypes the set of JPEG segments types that are to be returned. If this argument is <code>null</code>
	     *                     then all found segment types are returned.
	     */
	    @NotNull
	    public static JpegSegmentData readSegments(@NotNull File file, @Nullable Iterable<JpegSegmentType> segmentTypes) throws JpegProcessingException, IOException
	    {
	        FileInputStream stream = null;
	        try {
	            stream = new FileInputStream(file);
	            MetadataExtractor me = new MetadataExtractor();
	            return readSegments(me.new StreamReader(stream), segmentTypes);
	        } finally {
	            if (stream != null) {
	                stream.close();
	            }
	        }
	    }

	    /**
	     * Processes the provided JPEG data, and extracts the specified JPEG segments into a {@link JpegSegmentData} object.
	     * <p>
	     * Will not return SOS (start of scan) or EOI (end of image) segments.
	     *
	     * @param reader a {@link SequentialReader} from which the JPEG data will be read. It must be positioned at the
	     *               beginning of the JPEG data stream.
	     * @param segmentTypes the set of JPEG segments types that are to be returned. If this argument is <code>null</code>
	     *                     then all found segment types are returned.
	     */
	    @NotNull
	    public static JpegSegmentData readSegments(@NotNull final SequentialReader reader, @Nullable Iterable<JpegSegmentType> segmentTypes) throws JpegProcessingException, IOException
	    {
	        // Must be big-endian
	        assert (reader.isMotorolaByteOrder());

	        // first two bytes should be JPEG magic number
	        final int magicNumber = reader.getUInt16();
	        if (magicNumber != 0xFFD8) {
	        	MetadataExtractor me = new MetadataExtractor();
	            throw me.new JpegProcessingException("JPEG data is expected to begin with 0xFFD8 (Ã¿Ã˜) not 0x" + Integer.toHexString(magicNumber));
	        }

	        Set<Byte> segmentTypeBytes = null;
	        if (segmentTypes != null) {
	            segmentTypeBytes = new HashSet<Byte>();
	            for (JpegSegmentType segmentType : segmentTypes) {
	                segmentTypeBytes.add(segmentType.byteValue);
	            }
	        }

        	MetadataExtractor me = new MetadataExtractor();
	        JpegSegmentData segmentData = me.new JpegSegmentData();

	        do {
	            // Find the segment marker. Markers are zero or more 0xFF bytes, followed
	            // by a 0xFF and then a byte not equal to 0x00 or 0xFF.

	            byte segmentIdentifier = reader.getInt8();
	            byte segmentType = reader.getInt8();

	            // Read until we have a 0xFF byte followed by a byte that is not 0xFF or 0x00
	            while (segmentIdentifier != SEGMENT_IDENTIFIER || segmentType == SEGMENT_IDENTIFIER || segmentType == 0) {
	            	segmentIdentifier = segmentType;
	            	segmentType = reader.getInt8();
	            }

	            if (segmentType == SEGMENT_SOS) {
	                // The 'Start-Of-Scan' segment's length doesn't include the image data, instead would
	                // have to search for the two bytes: 0xFF 0xD9 (EOI).
	                // It comes last so simply return at this point
	                return segmentData;
	            }

	            if (segmentType == MARKER_EOI) {
	                // the 'End-Of-Image' segment -- this should never be found in this fashion
	                return segmentData;
	            }

	            // next 2-bytes are <segment-size>: [high-byte] [low-byte]
	            int segmentLength = reader.getUInt16();

	            // segment length includes size bytes, so subtract two
	            segmentLength -= 2;

	            if (segmentLength < 0) {
	                throw me.new JpegProcessingException("JPEG segment size would be less than zero");
	            }

	            // Check whether we are interested in this segment
	            if (segmentTypeBytes == null || segmentTypeBytes.contains(segmentType)) {
	                byte[] segmentBytes = reader.getBytes(segmentLength);
	                assert (segmentLength == segmentBytes.length);
	                segmentData.addSegment(segmentType, segmentBytes);
	            } else {
	                // Skip this segment
	                if (!reader.trySkip(segmentLength)) {
	                    // If skipping failed, just return the segments we found so far
	                    return segmentData;
	                }
	            }

	        } while (true);
	    }

	    private JpegSegmentReader() throws Exception
	    {
	        throw new Exception("Not intended for instantiation.");
	    }
	}
	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class FileSystemDescriptor extends TagDescriptor<FileSystemDirectory>
	{
		public static final int TAG_FILE_SIZE = 2;
	    public FileSystemDescriptor(@NotNull FileSystemDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case TAG_FILE_SIZE:
	                return getFileSizeDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    private String getFileSizeDescription()
	    {
	        Long size = _directory.getLongObject(TAG_FILE_SIZE);

	        if (size == null)
	            return null;

	        return Long.toString(size) + " bytes";
	    }
	}


	
	/**
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class FileSystemDirectory extends Directory
	{
	    public static final int TAG_FILE_NAME = 1;
	    public static final int TAG_FILE_SIZE = 2;
	    public static final int TAG_FILE_MODIFIED_DATE = 3;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TAG_FILE_NAME, "File Name");
	        _tagNameMap.put(TAG_FILE_SIZE, "File Size");
	        _tagNameMap.put(TAG_FILE_MODIFIED_DATE, "File Modified Date");
	    }

	    public FileSystemDirectory()
	    {
	        this.setDescriptor(new FileSystemDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "File";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}


	public class FileSystemMetadataReader
	{
	    public void read(@NotNull File file, @NotNull Metadata metadata) throws IOException
	    {
	        if (!file.isFile())
	            throw new IOException("File object must reference a file");
	        if (!file.exists())
	            throw new IOException("File does not exist");
	        if (!file.canRead())
	            throw new IOException("File is not readable");

	        FileSystemDirectory directory = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);

	        if (directory == null) {
	            directory = new FileSystemDirectory();
	            metadata.addDirectory(directory);
	        }

	        directory.setString(FileSystemDirectory.TAG_FILE_NAME, file.getName());
	        directory.setLong(FileSystemDirectory.TAG_FILE_SIZE, file.length());
	        directory.setDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE, new Date(file.lastModified()));
	    }
	}


	/**
	 * Obtains all available metadata from JPEG formatted files.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegMetadataReader
	{
		public JpegMetadataReader() {
			
		}
		MetadataExtractor me = new MetadataExtractor();
	    public final Iterable<JpegSegmentMetadataReader> ALL_READERS = Arrays.asList(
	    		
	            me.new JpegReader(),
	            me.new JpegCommentReader(),
	            /*
	            new JfifReader(),
	            new JfxxReader(),
	            */
	            me.new ExifReader()
	            /*
	            new XmpReader(),
	            new IccReader(),
	            new PhotoshopReader(),
	            new DuckyReader(),
	            new IptcReader(),
	            new AdobeJpegReader(),
	            new JpegDhtReader(),
	            new JpegDnlReader()
	            */
	    );

	    @NotNull
	    public Metadata readMetadata(@NotNull InputStream inputStream, @Nullable Iterable<JpegSegmentMetadataReader> readers) throws JpegProcessingException, IOException
	    {
	    	MetadataExtractor me = new MetadataExtractor();
	        Metadata metadata = me.new Metadata();
	        process(metadata, inputStream, readers);
	        return metadata;
	    }

	    @NotNull
	    public Metadata readMetadata(@NotNull InputStream inputStream) throws JpegProcessingException, IOException
	    {
	        return readMetadata(inputStream, null);
	    }

	    @NotNull
	    public Metadata readMetadata(@NotNull File file, @Nullable Iterable<JpegSegmentMetadataReader> readers) throws JpegProcessingException, IOException
	    {
	        InputStream inputStream = new FileInputStream(file);
	        Metadata metadata;
	        try {
	            metadata = readMetadata(inputStream, readers);
	        } finally {
	            inputStream.close();
	        }
	        MetadataExtractor me = new MetadataExtractor();
	        me.new FileSystemMetadataReader().read(file, metadata);
	        return metadata;
	    }

	    @NotNull
	    public Metadata readMetadata(@NotNull File file) throws JpegProcessingException, IOException
	    {
	        return readMetadata(file, null);
	    }

	    public void process(@NotNull Metadata metadata, @NotNull InputStream inputStream) throws JpegProcessingException, IOException
	    {
	        process(metadata, inputStream, null);
	    }

	    public void process(@NotNull Metadata metadata, @NotNull InputStream inputStream, @Nullable Iterable<JpegSegmentMetadataReader> readers) throws JpegProcessingException, IOException
	    {
	        if (readers == null)
	            readers = ALL_READERS;

	        Set<JpegSegmentType> segmentTypes = new HashSet<JpegSegmentType>();
	        for (JpegSegmentMetadataReader reader : readers) {
	            for (JpegSegmentType type : reader.getSegmentTypes()) {
	                segmentTypes.add(type);
	            }
	        }

	        MetadataExtractor me = new MetadataExtractor();
	        JpegSegmentData segmentData = JpegSegmentReader.readSegments(me.new StreamReader(inputStream), segmentTypes);

	        processJpegSegmentData(metadata, readers, segmentData);
	    }

	    public void processJpegSegmentData(Metadata metadata, Iterable<JpegSegmentMetadataReader> readers, JpegSegmentData segmentData)
	    {
	        // Pass the appropriate byte arrays to each reader.
	        for (JpegSegmentMetadataReader reader : readers) {
	            for (JpegSegmentType segmentType : reader.getSegmentTypes()) {
	                reader.readJpegSegments(segmentData.getSegments(segmentType), metadata, segmentType);
	            }
	        }
	    }

	    
	}
	
	/**
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class SequentialByteArrayReader extends SequentialReader
	{
	    @NotNull
	    private final byte[] _bytes;
	    private int _index;

	    @Override
	    public long getPosition()
	    {
	        return _index;
	    }

	    public SequentialByteArrayReader(@NotNull byte[] bytes)
	    {
	        this(bytes, 0);
	    }

	    //@SuppressWarnings("ConstantConditions")
	    public SequentialByteArrayReader(@NotNull byte[] bytes, int baseIndex)
	    {
	        if (bytes == null)
	            throw new NullPointerException();

	        _bytes = bytes;
	        _index = baseIndex;
	    }

	    @Override
	    public byte getByte() throws IOException
	    {
	        if (_index >= _bytes.length) {
	            throw new EOFException("End of data reached.");
	        }
	        return _bytes[_index++];
	    }

	    @NotNull
	    @Override
	    public byte[] getBytes(int count) throws IOException
	    {
	        if ((long)_index + count > _bytes.length) {
	            throw new EOFException("End of data reached.");
	        }

	        byte[] bytes = new byte[count];
	        System.arraycopy(_bytes, _index, bytes, 0, count);
	        _index += count;

	        return bytes;
	    }

	    @Override
	    public void getBytes(@NotNull byte[] buffer, int offset, int count) throws IOException
	    {
	        if ((long)_index + count > _bytes.length) {
	            throw new EOFException("End of data reached.");
	        }

	        System.arraycopy(_bytes, _index, buffer, offset, count);
	        _index += count;
	    }

	    @Override
	    public void skip(long n) throws IOException
	    {
	        if (n < 0) {
	            throw new IllegalArgumentException("n must be zero or greater.");
	        }

	        if (_index + n > _bytes.length) {
	            throw new EOFException("End of data reached.");
	        }

	        _index += n;
	    }

	    @Override
	    public boolean trySkip(long n) throws IOException
	    {
	        if (n < 0) {
	            throw new IllegalArgumentException("n must be zero or greater.");
	        }

	        if (_index + n > _bytes.length)  {
	            _index = _bytes.length;
	            return false;
	        }

	        _index += n;

	        return true;
	    }

	    @Override
	    public int available() {
	        return _bytes.length - _index;
	    }
	}
	
	/**
	 * Stores information about a JPEG image component such as the component id, horiz/vert sampling factor and
	 * quantization table number.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegComponent implements Serializable
	{
	    private static final long serialVersionUID = 61121257899091914L;

	    private final int _componentId;
	    private final int _samplingFactorByte;
	    private final int _quantizationTableNumber;

	    public JpegComponent(int componentId, int samplingFactorByte, int quantizationTableNumber)
	    {
	        _componentId = componentId;
	        _samplingFactorByte = samplingFactorByte;
	        _quantizationTableNumber = quantizationTableNumber;
	    }

	    public int getComponentId()
	    {
	        return _componentId;
	    }

	    /**
	     * Returns the component name (one of: Y, Cb, Cr, I, or Q)
	     * @return the component name
	     */
	    @NotNull
	    public String getComponentName()
	    {
	        switch (_componentId)
	        {
	            case 1:
	                return "Y";
	            case 2:
	                return "Cb";
	            case 3:
	                return "Cr";
	            case 4:
	                return "I";
	            case 5:
	                return "Q";
	            default:
	                return String.format("Unknown (%s)", _componentId);
	        }
	    }

	    public int getQuantizationTableNumber()
	    {
	        return _quantizationTableNumber;
	    }

	    public int getHorizontalSamplingFactor()
	    {
	        return (_samplingFactorByte>>4) & 0x0F;
	    }

	    public int getVerticalSamplingFactor()
	    {
	        return _samplingFactorByte & 0x0F;
	    }

	    @NotNull
	    @Override
	    public String toString() {
	        return String.format(
	            "Quantization table %d, Sampling factors %d horiz/%d vert",
	            _quantizationTableNumber,
	            getHorizontalSamplingFactor(),
	            getVerticalSamplingFactor()
	        );
	    }
	}
	
	/**
	 * Provides human-readable string versions of the tags stored in a JpegDirectory.
	 * Thanks to Darrell Silver (www.darrellsilver.com) for the initial version of this class.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class JpegDescriptor extends TagDescriptor<JpegDirectory>
	{
		public static final int TAG_COMPRESSION_TYPE = -3;
	    /** This is in bits/sample, usually 8 (12 and 16 not supported by most software). */
	    public static final int TAG_DATA_PRECISION = 0;
	    /** The image's height.  Necessary for decoding the image, so it should always be there. */
	    public static final int TAG_IMAGE_HEIGHT = 1;
	    /** The image's width.  Necessary for decoding the image, so it should always be there. */
	    public static final int TAG_IMAGE_WIDTH = 3;
	    /** the first of a possible 4 color components.  Number of components specified in TAG_NUMBER_OF_COMPONENTS. */
	    public static final int TAG_COMPONENT_DATA_1 = 6;
	    /** the second of a possible 4 color components.  Number of components specified in TAG_NUMBER_OF_COMPONENTS. */
	    public static final int TAG_COMPONENT_DATA_2 = 7;
	    /** the third of a possible 4 color components.  Number of components specified in TAG_NUMBER_OF_COMPONENTS. */
	    public static final int TAG_COMPONENT_DATA_3 = 8;
	    /** the fourth of a possible 4 color components.  Number of components specified in TAG_NUMBER_OF_COMPONENTS. */
	    public static final int TAG_COMPONENT_DATA_4 = 9;

	    public JpegDescriptor(@NotNull JpegDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType)
	        {
	            case TAG_COMPRESSION_TYPE:
	                return getImageCompressionTypeDescription();
	            case TAG_COMPONENT_DATA_1:
	                return getComponentDataDescription(0);
	            case TAG_COMPONENT_DATA_2:
	                return getComponentDataDescription(1);
	            case TAG_COMPONENT_DATA_3:
	                return getComponentDataDescription(2);
	            case TAG_COMPONENT_DATA_4:
	                return getComponentDataDescription(3);
	            case TAG_DATA_PRECISION:
	                return getDataPrecisionDescription();
	            case TAG_IMAGE_HEIGHT:
	                return getImageHeightDescription();
	            case TAG_IMAGE_WIDTH:
	                return getImageWidthDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getImageCompressionTypeDescription()
	    {
	        return getIndexedDescription(TAG_COMPRESSION_TYPE,
	            "Baseline",
	            "Extended sequential, Huffman",
	            "Progressive, Huffman",
	            "Lossless, Huffman",
	            null, // no 4
	            "Differential sequential, Huffman",
	            "Differential progressive, Huffman",
	            "Differential lossless, Huffman",
	            "Reserved for JPEG extensions",
	            "Extended sequential, arithmetic",
	            "Progressive, arithmetic",
	            "Lossless, arithmetic",
	            null, // no 12
	            "Differential sequential, arithmetic",
	            "Differential progressive, arithmetic",
	            "Differential lossless, arithmetic");
	    }

	    @Nullable
	    public String getImageWidthDescription()
	    {
	        final String value = _directory.getString(TAG_IMAGE_WIDTH);
	        if (value==null)
	            return null;
	        return value + " pixels";
	    }

	    @Nullable
	    public String getImageHeightDescription()
	    {
	        final String value = _directory.getString(TAG_IMAGE_HEIGHT);
	        if (value==null)
	            return null;
	        return value + " pixels";
	    }

	    @Nullable
	    public String getDataPrecisionDescription()
	    {
	        final String value = _directory.getString(TAG_DATA_PRECISION);
	        if (value==null)
	            return null;
	        return value + " bits";
	    }

	    @Nullable
	    public String getComponentDataDescription(int componentNumber)
	    {
	        JpegComponent value = _directory.getComponent(componentNumber);

	        if (value==null)
	            return null;

	        return value.getComponentName() + " component: " + value;
	    }
	}

	
	/**
	 * Directory of tags and values for the SOF0 JPEG segment.  This segment holds basic metadata about the image.
	 *
	 * @author Darrell Silver http://www.darrellsilver.com and Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class JpegDirectory extends Directory
	{
	    public static final int TAG_COMPRESSION_TYPE = -3;
	    /** This is in bits/sample, usually 8 (12 and 16 not supported by most software). */
	    public static final int TAG_DATA_PRECISION = 0;
	    /** The image's height.  Necessary for decoding the image, so it should always be there. */
	    public static final int TAG_IMAGE_HEIGHT = 1;
	    /** The image's width.  Necessary for decoding the image, so it should always be there. */
	    public static final int TAG_IMAGE_WIDTH = 3;
	    /**
	     * Usually 1 = grey scaled, 3 = color YcbCr or YIQ, 4 = color CMYK
	     * Each component TAG_COMPONENT_DATA_[1-4], has the following meaning:
	     * component Id(1byte)(1 = Y, 2 = Cb, 3 = Cr, 4 = I, 5 = Q),
	     * sampling factors (1byte) (bit 0-3 vertical., 4-7 horizontal.),
	     * quantization table number (1 byte).
	     * <p>
	     * This info is from http://www.funducode.com/freec/Fileformats/format3/format3b.htm
	     */
	    public static final int TAG_NUMBER_OF_COMPONENTS = 5;

	    // NOTE!  Component tag type int values must increment in steps of 1

	    /** the first of a possible 4 color components.  Number of components specified in TAG_NUMBER_OF_COMPONENTS. */
	    public static final int TAG_COMPONENT_DATA_1 = 6;
	    /** the second of a possible 4 color components.  Number of components specified in TAG_NUMBER_OF_COMPONENTS. */
	    public static final int TAG_COMPONENT_DATA_2 = 7;
	    /** the third of a possible 4 color components.  Number of components specified in TAG_NUMBER_OF_COMPONENTS. */
	    public static final int TAG_COMPONENT_DATA_3 = 8;
	    /** the fourth of a possible 4 color components.  Number of components specified in TAG_NUMBER_OF_COMPONENTS. */
	    public static final int TAG_COMPONENT_DATA_4 = 9;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TAG_COMPRESSION_TYPE, "Compression Type");
	        _tagNameMap.put(TAG_DATA_PRECISION, "Data Precision");
	        _tagNameMap.put(TAG_IMAGE_WIDTH, "Image Width");
	        _tagNameMap.put(TAG_IMAGE_HEIGHT, "Image Height");
	        _tagNameMap.put(TAG_NUMBER_OF_COMPONENTS, "Number of Components");
	        _tagNameMap.put(TAG_COMPONENT_DATA_1, "Component 1");
	        _tagNameMap.put(TAG_COMPONENT_DATA_2, "Component 2");
	        _tagNameMap.put(TAG_COMPONENT_DATA_3, "Component 3");
	        _tagNameMap.put(TAG_COMPONENT_DATA_4, "Component 4");
	    }

	    public JpegDirectory()
	    {
	        this.setDescriptor(new JpegDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "JPEG";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }

	    /**
	     * @param componentNumber The zero-based index of the component.  This number is normally between 0 and 3.
	     *                        Use getNumberOfComponents for bounds-checking.
	     * @return the JpegComponent having the specified number.
	     */
	    @Nullable
	    public JpegComponent getComponent(int componentNumber)
	    {
	        int tagType = JpegDirectory.TAG_COMPONENT_DATA_1 + componentNumber;
	        return (JpegComponent)getObject(tagType);
	    }

	    public int getImageWidth() throws MetadataException
	    {
	        return getInt(JpegDirectory.TAG_IMAGE_WIDTH);
	    }

	    public int getImageHeight() throws MetadataException
	    {
	        return getInt(JpegDirectory.TAG_IMAGE_HEIGHT);
	    }

	    public int getNumberOfComponents() throws MetadataException
	    {
	        return getInt(JpegDirectory.TAG_NUMBER_OF_COMPONENTS);
	    }
	}



	/**
	 * Decodes JPEG SOFn data, populating a {@link Metadata} object with tag values in a {@link JpegDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 * @author Darrell Silver http://www.darrellsilver.com
	 */
	public class JpegReader implements JpegSegmentMetadataReader
	{
	    @NotNull
	    public Iterable<JpegSegmentType> getSegmentTypes()
	    {
	        // NOTE that some SOFn values do not exist
	        return Arrays.asList(
	            JpegSegmentType.SOF0,
	            JpegSegmentType.SOF1,
	            JpegSegmentType.SOF2,
	            JpegSegmentType.SOF3,
//	            JpegSegmentType.SOF4,
	            JpegSegmentType.SOF5,
	            JpegSegmentType.SOF6,
	            JpegSegmentType.SOF7,
//	            JpegSegmentType.JPG,
	            JpegSegmentType.SOF9,
	            JpegSegmentType.SOF10,
	            JpegSegmentType.SOF11,
//	            JpegSegmentType.SOF12,
	            JpegSegmentType.SOF13,
	            JpegSegmentType.SOF14,
	            JpegSegmentType.SOF15
	        );
	    }

	    public void readJpegSegments(@NotNull Iterable<byte[]> segments, @NotNull Metadata metadata, @NotNull JpegSegmentType segmentType)
	    {
	        for (byte[] segmentBytes : segments) {
	            extract(segmentBytes, metadata, segmentType);
	        }
	    }

	    public void extract(byte[] segmentBytes, Metadata metadata, JpegSegmentType segmentType)
	    {
	        JpegDirectory directory = new JpegDirectory();
	        metadata.addDirectory(directory);

	        // The value of TAG_COMPRESSION_TYPE is determined by the segment type found
	        directory.setInt(JpegDirectory.TAG_COMPRESSION_TYPE, segmentType.byteValue - JpegSegmentType.SOF0.byteValue);

	        SequentialReader reader = new SequentialByteArrayReader(segmentBytes);

	        try {
	            directory.setInt(JpegDirectory.TAG_DATA_PRECISION, reader.getUInt8());
	            directory.setInt(JpegDirectory.TAG_IMAGE_HEIGHT, reader.getUInt16());
	            directory.setInt(JpegDirectory.TAG_IMAGE_WIDTH, reader.getUInt16());
	            short componentCount = reader.getUInt8();
	            directory.setInt(JpegDirectory.TAG_NUMBER_OF_COMPONENTS, componentCount);

	            // for each component, there are three bytes of data:
	            // 1 - Component ID: 1 = Y, 2 = Cb, 3 = Cr, 4 = I, 5 = Q
	            // 2 - Sampling factors: bit 0-3 vertical, 4-7 horizontal
	            // 3 - Quantization table number
	            for (int i = 0; i < (int)componentCount; i++) {
	                final int componentId = reader.getUInt8();
	                final int samplingFactorByte = reader.getUInt8();
	                final int quantizationTableNumber = reader.getUInt8();
	                final JpegComponent component = new JpegComponent(componentId, samplingFactorByte, quantizationTableNumber);
	                directory.setObject(JpegDirectory.TAG_COMPONENT_DATA_1 + i, component);
	            }
	        } catch (IOException ex) {
	            directory.addError(ex.getMessage());
	        }
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link JpegCommentDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class JpegCommentDescriptor extends TagDescriptor<JpegCommentDirectory>
	{
	    public JpegCommentDescriptor(@NotNull JpegCommentDirectory directory)
	    {
	        super(directory);
	    }

	    @Nullable
	    public String getJpegCommentDescription()
	    {
	        return _directory.getString(JpegCommentDirectory.TAG_COMMENT);
	    }
	}

	
	/**
	 * Describes tags used by a JPEG file comment.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class JpegCommentDirectory extends Directory
	{
	    /**
	     * This value does not apply to a particular standard. Rather, this value has been fabricated to maintain
	     * consistency with other directory types.
	     */
	    public static final int TAG_COMMENT = 0;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TAG_COMMENT, "JPEG Comment");
	    }

	    public JpegCommentDirectory()
	    {
	        this.setDescriptor(new JpegCommentDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "JpegComment";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}

	
	/**
	 * Decodes the comment stored within JPEG files, populating a {@link Metadata} object with tag values in a
	 * {@link JpegCommentDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class JpegCommentReader implements JpegSegmentMetadataReader
	{
	    @NotNull
	    public Iterable<JpegSegmentType> getSegmentTypes()
	    {
	        return Collections.singletonList(JpegSegmentType.COM);
	    }

	    public void readJpegSegments(@NotNull Iterable<byte[]> segments, @NotNull Metadata metadata, @NotNull JpegSegmentType segmentType)
	    {
	        for (byte[] segmentBytes : segments) {
	            JpegCommentDirectory directory = new JpegCommentDirectory();
	            metadata.addDirectory(directory);

	            // The entire contents of the directory are the comment
	            directory.setStringValue(JpegCommentDirectory.TAG_COMMENT, new StringValue(segmentBytes, null));
	        }
	    }
	}
	
	/**
	 * Base class for random access data reading operations of common data types.
	 * <p>
	 * By default, the reader operates with Motorola byte order (big endianness).  This can be changed by calling
	 * {@link com.drew.lang.RandomAccessReader#setMotorolaByteOrder(boolean)}.
	 * <p>
	 * Concrete implementations include:
	 * <ul>
	 *     <li>{@link ByteArrayReader}</li>
	 *     <li>{@link RandomAccessStreamReader}</li>
	 * </ul>
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public abstract class RandomAccessReader
	{
	    private boolean _isMotorolaByteOrder = true;

	    public abstract int toUnshiftedOffset(int localOffset);

	    /**
	     * Gets the byte value at the specified byte <code>index</code>.
	     * <p>
	     * Implementations should not perform any bounds checking in this method. That should be performed
	     * in <code>validateIndex</code> and <code>isValidIndex</code>.
	     *
	     * @param index The index from which to read the byte
	     * @return The read byte value
	     * @throws IllegalArgumentException <code>index</code> is negative
	     * @throws BufferBoundsException if the requested byte is beyond the end of the underlying data source
	     * @throws IOException if the byte is unable to be read
	     */
	    public abstract byte getByte(int index) throws IOException;

	    /**
	     * Returns the required number of bytes from the specified index from the underlying source.
	     *
	     * @param index The index from which the bytes begins in the underlying source
	     * @param count The number of bytes to be returned
	     * @return The requested bytes
	     * @throws IllegalArgumentException <code>index</code> or <code>count</code> are negative
	     * @throws BufferBoundsException if the requested bytes extend beyond the end of the underlying data source
	     * @throws IOException if the byte is unable to be read
	     */
	    @NotNull
	    public abstract byte[] getBytes(int index, int count) throws IOException;

	    /**
	     * Ensures that the buffered bytes extend to cover the specified index. If not, an attempt is made
	     * to read to that point.
	     * <p>
	     * If the stream ends before the point is reached, a {@link BufferBoundsException} is raised.
	     *
	     * @param index the index from which the required bytes start
	     * @param bytesRequested the number of bytes which are required
	     * @throws IOException if the stream ends before the required number of bytes are acquired
	     */
	    protected abstract void validateIndex(int index, int bytesRequested) throws IOException;

	    protected abstract boolean isValidIndex(int index, int bytesRequested) throws IOException;

	    /**
	     * Returns the length of the data source in bytes.
	     * <p>
	     * This is a simple operation for implementations (such as {@link RandomAccessFileReader} and
	     * {@link ByteArrayReader}) that have the entire data source available.
	     * <p>
	     * Users of this method must be aware that sequentially accessed implementations such as
	     * {@link RandomAccessStreamReader} will have to read and buffer the entire data source in
	     * order to determine the length.
	     *
	     * @return the length of the data source, in bytes.
	     */
	    public abstract long getLength() throws IOException;

	    /**
	     * Sets the endianness of this reader.
	     * <ul>
	     * <li><code>true</code> for Motorola (or big) endianness (also known as network byte order), with MSB before LSB.</li>
	     * <li><code>false</code> for Intel (or little) endianness, with LSB before MSB.</li>
	     * </ul>
	     *
	     * @param motorolaByteOrder <code>true</code> for Motorola/big endian, <code>false</code> for Intel/little endian
	     */
	    public void setMotorolaByteOrder(boolean motorolaByteOrder)
	    {
	        _isMotorolaByteOrder = motorolaByteOrder;
	    }

	    /**
	     * Gets the endianness of this reader.
	     * <ul>
	     * <li><code>true</code> for Motorola (or big) endianness (also known as network byte order), with MSB before LSB.</li>
	     * <li><code>false</code> for Intel (or little) endianness, with LSB before MSB.</li>
	     * </ul>
	     */
	    public boolean isMotorolaByteOrder()
	    {
	        return _isMotorolaByteOrder;
	    }

	    /**
	     * Gets whether a bit at a specific index is set or not.
	     *
	     * @param index the number of bits at which to test
	     * @return true if the bit is set, otherwise false
	     * @throws IOException the buffer does not contain enough bytes to service the request, or index is negative
	     */
	    public boolean getBit(int index) throws IOException
	    {
	        int byteIndex = index / 8;
	        int bitIndex = index % 8;

	        validateIndex(byteIndex, 1);

	        byte b = getByte(byteIndex);
	        return ((b >> bitIndex) & 1) == 1;
	    }

	    /**
	     * Returns an unsigned 8-bit int calculated from one byte of data at the specified index.
	     *
	     * @param index position within the data buffer to read byte
	     * @return the 8 bit int value, between 0 and 255
	     * @throws IOException the buffer does not contain enough bytes to service the request, or index is negative
	     */
	    public short getUInt8(int index) throws IOException
	    {
	        validateIndex(index, 1);

	        return (short) (getByte(index) & 0xFF);
	    }

	    /**
	     * Returns a signed 8-bit int calculated from one byte of data at the specified index.
	     *
	     * @param index position within the data buffer to read byte
	     * @return the 8 bit int value, between 0x00 and 0xFF
	     * @throws IOException the buffer does not contain enough bytes to service the request, or index is negative
	     */
	    public byte getInt8(int index) throws IOException
	    {
	        validateIndex(index, 1);

	        return getByte(index);
	    }

	    /**
	     * Returns an unsigned 16-bit int calculated from two bytes of data at the specified index.
	     *
	     * @param index position within the data buffer to read first byte
	     * @return the 16 bit int value, between 0x0000 and 0xFFFF
	     * @throws IOException the buffer does not contain enough bytes to service the request, or index is negative
	     */
	    public int getUInt16(int index) throws IOException
	    {
	        validateIndex(index, 2);

	        if (_isMotorolaByteOrder) {
	            // Motorola - MSB first
	            return (getByte(index    ) << 8 & 0xFF00) |
	                   (getByte(index + 1)      & 0xFF);
	        } else {
	            // Intel ordering - LSB first
	            return (getByte(index + 1) << 8 & 0xFF00) |
	                   (getByte(index    )      & 0xFF);
	        }
	    }

	    /**
	     * Returns a signed 16-bit int calculated from two bytes of data at the specified index (MSB, LSB).
	     *
	     * @param index position within the data buffer to read first byte
	     * @return the 16 bit int value, between 0x0000 and 0xFFFF
	     * @throws IOException the buffer does not contain enough bytes to service the request, or index is negative
	     */
	    public short getInt16(int index) throws IOException
	    {
	        validateIndex(index, 2);

	        if (_isMotorolaByteOrder) {
	            // Motorola - MSB first
	            return (short) (((short)getByte(index    ) << 8 & (short)0xFF00) |
	                            ((short)getByte(index + 1)      & (short)0xFF));
	        } else {
	            // Intel ordering - LSB first
	            return (short) (((short)getByte(index + 1) << 8 & (short)0xFF00) |
	                            ((short)getByte(index    )      & (short)0xFF));
	        }
	    }

	    /**
	     * Get a 24-bit unsigned integer from the buffer, returning it as an int.
	     *
	     * @param index position within the data buffer to read first byte
	     * @return the unsigned 24-bit int value as a long, between 0x00000000 and 0x00FFFFFF
	     * @throws IOException the buffer does not contain enough bytes to service the request, or index is negative
	     */
	    public int getInt24(int index) throws IOException
	    {
	        validateIndex(index, 3);

	        if (_isMotorolaByteOrder) {
	            // Motorola - MSB first (big endian)
	            return (((int)getByte(index    )) << 16 & 0xFF0000) |
	                   (((int)getByte(index + 1)) << 8  & 0xFF00) |
	                   (((int)getByte(index + 2))       & 0xFF);
	        } else {
	            // Intel ordering - LSB first (little endian)
	            return (((int)getByte(index + 2)) << 16 & 0xFF0000) |
	                   (((int)getByte(index + 1)) << 8  & 0xFF00) |
	                   (((int)getByte(index    ))       & 0xFF);
	        }
	    }

	    /**
	     * Get a 32-bit unsigned integer from the buffer, returning it as a long.
	     *
	     * @param index position within the data buffer to read first byte
	     * @return the unsigned 32-bit int value as a long, between 0x00000000 and 0xFFFFFFFF
	     * @throws IOException the buffer does not contain enough bytes to service the request, or index is negative
	     */
	    public long getUInt32(int index) throws IOException
	    {
	        validateIndex(index, 4);

	        if (_isMotorolaByteOrder) {
	            // Motorola - MSB first (big endian)
	            return (((long)getByte(index    )) << 24 & 0xFF000000L) |
	                   (((long)getByte(index + 1)) << 16 & 0xFF0000L) |
	                   (((long)getByte(index + 2)) << 8  & 0xFF00L) |
	                   (((long)getByte(index + 3))       & 0xFFL);
	        } else {
	            // Intel ordering - LSB first (little endian)
	            return (((long)getByte(index + 3)) << 24 & 0xFF000000L) |
	                   (((long)getByte(index + 2)) << 16 & 0xFF0000L) |
	                   (((long)getByte(index + 1)) << 8  & 0xFF00L) |
	                   (((long)getByte(index    ))       & 0xFFL);
	        }
	    }

	    /**
	     * Returns a signed 32-bit integer from four bytes of data at the specified index the buffer.
	     *
	     * @param index position within the data buffer to read first byte
	     * @return the signed 32 bit int value, between 0x00000000 and 0xFFFFFFFF
	     * @throws IOException the buffer does not contain enough bytes to service the request, or index is negative
	     */
	    public int getInt32(int index) throws IOException
	    {
	        validateIndex(index, 4);

	        if (_isMotorolaByteOrder) {
	            // Motorola - MSB first (big endian)
	            return (getByte(index    ) << 24 & 0xFF000000) |
	                   (getByte(index + 1) << 16 & 0xFF0000) |
	                   (getByte(index + 2) << 8  & 0xFF00) |
	                   (getByte(index + 3)       & 0xFF);
	        } else {
	            // Intel ordering - LSB first (little endian)
	            return (getByte(index + 3) << 24 & 0xFF000000) |
	                   (getByte(index + 2) << 16 & 0xFF0000) |
	                   (getByte(index + 1) << 8  & 0xFF00) |
	                   (getByte(index    )       & 0xFF);
	        }
	    }

	    /**
	     * Get a signed 64-bit integer from the buffer.
	     *
	     * @param index position within the data buffer to read first byte
	     * @return the 64 bit int value, between 0x0000000000000000 and 0xFFFFFFFFFFFFFFFF
	     * @throws IOException the buffer does not contain enough bytes to service the request, or index is negative
	     */
	    public long getInt64(int index) throws IOException
	    {
	        validateIndex(index, 8);

	        if (_isMotorolaByteOrder) {
	            // Motorola - MSB first
	            return ((long)getByte(index    ) << 56 & 0xFF00000000000000L) |
	                   ((long)getByte(index + 1) << 48 & 0xFF000000000000L) |
	                   ((long)getByte(index + 2) << 40 & 0xFF0000000000L) |
	                   ((long)getByte(index + 3) << 32 & 0xFF00000000L) |
	                   ((long)getByte(index + 4) << 24 & 0xFF000000L) |
	                   ((long)getByte(index + 5) << 16 & 0xFF0000L) |
	                   ((long)getByte(index + 6) << 8  & 0xFF00L) |
	                   ((long)getByte(index + 7)       & 0xFFL);
	        } else {
	            // Intel ordering - LSB first
	            return ((long)getByte(index + 7) << 56 & 0xFF00000000000000L) |
	                   ((long)getByte(index + 6) << 48 & 0xFF000000000000L) |
	                   ((long)getByte(index + 5) << 40 & 0xFF0000000000L) |
	                   ((long)getByte(index + 4) << 32 & 0xFF00000000L) |
	                   ((long)getByte(index + 3) << 24 & 0xFF000000L) |
	                   ((long)getByte(index + 2) << 16 & 0xFF0000L) |
	                   ((long)getByte(index + 1) << 8  & 0xFF00L) |
	                   ((long)getByte(index    )       & 0xFFL);
	        }
	    }

	    /**
	     * Gets a s15.16 fixed point float from the buffer.
	     * <p>
	     * This particular fixed point encoding has one sign bit, 15 numerator bits and 16 denominator bits.
	     *
	     * @return the floating point value
	     * @throws IOException the buffer does not contain enough bytes to service the request, or index is negative
	     */
	    public float getS15Fixed16(int index) throws IOException
	    {
	        validateIndex(index, 4);

	        if (_isMotorolaByteOrder) {
	            float res = (getByte(index    ) & 0xFF) << 8 |
	                        (getByte(index + 1) & 0xFF);
	            int d =     (getByte(index + 2) & 0xFF) << 8 |
	                        (getByte(index + 3) & 0xFF);
	            return (float)(res + d/65536.0);
	        } else {
	            // this particular branch is untested
	            float res = (getByte(index + 3) & 0xFF) << 8 |
	                        (getByte(index + 2) & 0xFF);
	            int d =     (getByte(index + 1) & 0xFF) << 8 |
	                        (getByte(index    ) & 0xFF);
	            return (float)(res + d/65536.0);
	        }
	    }

	    public float getFloat32(int index) throws IOException
	    {
	        return Float.intBitsToFloat(getInt32(index));
	    }

	    public double getDouble64(int index) throws IOException
	    {
	        return Double.longBitsToDouble(getInt64(index));
	    }

	    @NotNull
	    public StringValue getStringValue(int index, int bytesRequested, @Nullable Charset charset) throws IOException
	    {
	        return new StringValue(getBytes(index, bytesRequested), charset);
	    }

	    @NotNull
	    public String getString(int index, int bytesRequested, @NotNull Charset charset) throws IOException
	    {
	        return new String(getBytes(index, bytesRequested), charset.name());
	    }

	    @NotNull
	    public String getString(int index, int bytesRequested, @NotNull String charset) throws IOException
	    {
	        byte[] bytes = getBytes(index, bytesRequested);
	        try {
	            return new String(bytes, charset);
	        } catch (UnsupportedEncodingException e) {
	            return new String(bytes);
	        }
	    }

	    /**
	     * Creates a String from the _data buffer starting at the specified index,
	     * and ending where <code>byte=='\0'</code> or where <code>length==maxLength</code>.
	     *
	     * @param index          The index within the buffer at which to start reading the string.
	     * @param maxLengthBytes The maximum number of bytes to read.  If a zero-byte is not reached within this limit,
	     *                       reading will stop and the string will be truncated to this length.
	     * @return The read string.
	     * @throws IOException The buffer does not contain enough bytes to satisfy this request.
	     */
	    @NotNull
	    public String getNullTerminatedString(int index, int maxLengthBytes, @NotNull Charset charset) throws IOException
	    {
	        return new String(getNullTerminatedBytes(index, maxLengthBytes), charset.name());
	    }

	    @NotNull
	    public StringValue getNullTerminatedStringValue(int index, int maxLengthBytes, @Nullable Charset charset) throws IOException
	    {
	        byte[] bytes = getNullTerminatedBytes(index, maxLengthBytes);

	        return new StringValue(bytes, charset);
	    }

	    /**
	     * Returns the sequence of bytes punctuated by a <code>\0</code> value.
	     *
	     * @param index The index within the buffer at which to start reading the string.
	     * @param maxLengthBytes The maximum number of bytes to read. If a <code>\0</code> byte is not reached within this limit,
	     * the returned array will be <code>maxLengthBytes</code> long.
	     * @return The read byte array, excluding the null terminator.
	     * @throws IOException The buffer does not contain enough bytes to satisfy this request.
	     */
	    @NotNull
	    public byte[] getNullTerminatedBytes(int index, int maxLengthBytes) throws IOException
	    {
	        byte[] buffer = getBytes(index, maxLengthBytes);

	        // Count the number of non-null bytes
	        int length = 0;
	        while (length < buffer.length && buffer[length] != 0)
	            length++;

	        if (length == maxLengthBytes)
	            return buffer;

	        byte[] bytes = new byte[length];
	        if (length > 0)
	            System.arraycopy(buffer, 0, bytes, 0, length);
	        return bytes;
	    }
	}
	
	/**
	 * A checked replacement for {@link IndexOutOfBoundsException}.  Used by {@link RandomAccessReader}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public final static class BufferBoundsException extends IOException
	{
	    private static final long serialVersionUID = 2911102837808946396L;

	    public BufferBoundsException(int index, int bytesRequested, long bufferLength)
	    {
	        super(getMessage(index, bytesRequested, bufferLength));
	    }

	    public BufferBoundsException(final String message)
	    {
	        super(message);
	    }

	    private static String getMessage(int index, int bytesRequested, long bufferLength)
	    {
	        if (index < 0)
	            return String.format("Attempt to read from buffer using a negative index (%d)", index);

	        if (bytesRequested < 0)
	            return String.format("Number of requested bytes cannot be negative (%d)", bytesRequested);

	        if ((long)index + (long)bytesRequested - 1L > (long)Integer.MAX_VALUE)
	            return String.format("Number of requested bytes summed with starting index exceed maximum range of signed 32 bit integers (requested index: %d, requested count: %d)", index, bytesRequested);

	        return String.format("Attempt to read from beyond end of underlying data source (requested index: %d, requested count: %d, max index: %d)",
	                index, bytesRequested, bufferLength - 1);
	    }
	}


	/**
	 * Provides methods to read specific values from a byte array, with a consistent, checked exception structure for
	 * issues.
	 * <p>
	 * By default, the reader operates with Motorola byte order (big endianness).  This can be changed by calling
	 * <code>setMotorolaByteOrder(boolean)</code>.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 * */
	public class ByteArrayReader extends RandomAccessReader
	{
	    @NotNull
	    private final byte[] _buffer;
	    private final int _baseOffset;

	    //@SuppressWarnings({ "ConstantConditions" })
	    //@com.drew.lang.annotations.SuppressWarnings(value = "EI_EXPOSE_REP2", justification = "Design intent")
	    public ByteArrayReader(@NotNull byte[] buffer)
	    {
	        this(buffer, 0);
	    }

	    //@SuppressWarnings({ "ConstantConditions" })
	    //@com.drew.lang.annotations.SuppressWarnings(value = "EI_EXPOSE_REP2", justification = "Design intent")
	    public ByteArrayReader(@NotNull byte[] buffer, int baseOffset)
	    {
	        if (buffer == null)
	            throw new NullPointerException();
	        if (baseOffset < 0)
	            throw new IllegalArgumentException("Must be zero or greater");

	        _buffer = buffer;
	        _baseOffset = baseOffset;
	    }

	    @Override
	    public int toUnshiftedOffset(int localOffset)
	    {
	        return localOffset + _baseOffset;
	    }

	    @Override
	    public long getLength()
	    {
	        return _buffer.length - _baseOffset;
	    }

	    @Override
	    public byte getByte(int index) throws IOException
	    {
	        validateIndex(index, 1);
	        return _buffer[index + _baseOffset];
	    }

	    @Override
	    protected void validateIndex(int index, int bytesRequested) throws IOException
	    {
	        if (!isValidIndex(index, bytesRequested))
	            throw new BufferBoundsException(toUnshiftedOffset(index), bytesRequested, _buffer.length);
	    }

	    @Override
	    protected boolean isValidIndex(int index, int bytesRequested) throws IOException
	    {
	        return bytesRequested >= 0
	            && index >= 0
	            && (long)index + (long)bytesRequested - 1L < getLength();
	    }

	    @Override
	    @NotNull
	    public byte[] getBytes(int index, int count) throws IOException
	    {
	        validateIndex(index, count);

	        byte[] bytes = new byte[count];
	        System.arraycopy(_buffer, index + _baseOffset, bytes, 0, count);
	        return bytes;
	    }
	}

	/**
	 * An exception class thrown upon unexpected and fatal conditions while processing a TIFF file.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 * @author Darren Salomons
	 */
	public class TiffProcessingException extends ImageProcessingException
	{
	    private static final long serialVersionUID = -1658134119488001891L;

	    public TiffProcessingException(@Nullable String message)
	    {
	        super(message);
	    }

	    public TiffProcessingException(@Nullable String message, @Nullable Throwable cause)
	    {
	        super(message, cause);
	    }

	    public TiffProcessingException(@Nullable Throwable cause)
	    {
	        super(cause);
	    }
	}
	
	/**
	 * Interface of an class capable of handling events raised during the reading of a TIFF file
	 * via {@link TiffReader}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public interface TiffHandler
	{
	    /**
	     * Receives the 2-byte marker found in the TIFF header.
	     * <p>
	     * Implementations are not obligated to use this information for any purpose, though it may be useful for
	     * validation or perhaps differentiating the type of mapping to use for observed tags and IFDs.
	     *
	     * @param marker the 2-byte value found at position 2 of the TIFF header
	     */
	    void setTiffMarker(int marker) throws TiffProcessingException;

	    boolean tryEnterSubIfd(int tagId);
	    boolean hasFollowerIfd();

	    void endingIFD();

	    @Nullable
	    Long tryCustomProcessFormat(int tagId, int formatCode, long componentCount);

	    boolean customProcessTag(int tagOffset,
	                             @NotNull Set<Integer> processedIfdOffsets,
	                             int tiffHeaderOffset,
	                             @NotNull RandomAccessReader reader,
	                             int tagId,
	                             int byteCount) throws IOException;

	    void warn(@NotNull String message);
	    void error(@NotNull String message);

	    void setByteArray(int tagId, @NotNull byte[] bytes);
	    void setString(int tagId, @NotNull StringValue string);
	    void setRational(int tagId, @NotNull Rational rational);
	    void setRationalArray(int tagId, @NotNull Rational[] array);
	    void setFloat(int tagId, float float32);
	    void setFloatArray(int tagId, @NotNull float[] array);
	    void setDouble(int tagId, double double64);
	    void setDoubleArray(int tagId, @NotNull double[] array);
	    void setInt8s(int tagId, byte int8s);
	    void setInt8sArray(int tagId, @NotNull byte[] array);
	    void setInt8u(int tagId, short int8u);
	    void setInt8uArray(int tagId, @NotNull short[] array);
	    void setInt16s(int tagId, int int16s);
	    void setInt16sArray(int tagId, @NotNull short[] array);
	    void setInt16u(int tagId, int int16u);
	    void setInt16uArray(int tagId, @NotNull int[] array);
	    void setInt32s(int tagId, int int32s);
	    void setInt32sArray(int tagId, @NotNull int[] array);
	    void setInt32u(int tagId, long int32u);
	    void setInt32uArray(int tagId, @NotNull long[] array);
	}
	
	/**
	 * An enumeration of data formats used by the TIFF specification.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class TiffDataFormat
	{
	    public static final int CODE_INT8_U = 1;
	    public static final int CODE_STRING = 2;
	    public static final int CODE_INT16_U = 3;
	    public static final int CODE_INT32_U = 4;
	    public static final int CODE_RATIONAL_U = 5;
	    public static final int CODE_INT8_S = 6;
	    public static final int CODE_UNDEFINED = 7;
	    public static final int CODE_INT16_S = 8;
	    public static final int CODE_INT32_S = 9;
	    public static final int CODE_RATIONAL_S = 10;
	    public static final int CODE_SINGLE = 11;
	    public static final int CODE_DOUBLE = 12;

	    @NotNull public final TiffDataFormat INT8_U = new TiffDataFormat("BYTE", CODE_INT8_U, 1);
	    @NotNull public final TiffDataFormat STRING = new TiffDataFormat("STRING", CODE_STRING, 1);
	    @NotNull public final TiffDataFormat INT16_U = new TiffDataFormat("USHORT", CODE_INT16_U, 2);
	    @NotNull public final TiffDataFormat INT32_U = new TiffDataFormat("ULONG", CODE_INT32_U, 4);
	    @NotNull public final TiffDataFormat RATIONAL_U = new TiffDataFormat("URATIONAL", CODE_RATIONAL_U, 8);
	    @NotNull public final TiffDataFormat INT8_S = new TiffDataFormat("SBYTE", CODE_INT8_S, 1);
	    @NotNull public final TiffDataFormat UNDEFINED = new TiffDataFormat("UNDEFINED", CODE_UNDEFINED, 1);
	    @NotNull public final TiffDataFormat INT16_S = new TiffDataFormat("SSHORT", CODE_INT16_S, 2);
	    @NotNull public final TiffDataFormat INT32_S = new TiffDataFormat("SLONG", CODE_INT32_S, 4);
	    @NotNull public final TiffDataFormat RATIONAL_S = new TiffDataFormat("SRATIONAL", CODE_RATIONAL_S, 8);
	    @NotNull public final TiffDataFormat SINGLE = new TiffDataFormat("SINGLE", CODE_SINGLE, 4);
	    @NotNull public final TiffDataFormat DOUBLE = new TiffDataFormat("DOUBLE", CODE_DOUBLE, 8);

	    @NotNull
	    private String _name;
	    private int _tiffFormatCode;
	    private int _componentSizeBytes;
	    
	    public TiffDataFormat() {
	    	
	    }

	    @Nullable
	    public TiffDataFormat fromTiffFormatCode(int tiffFormatCode)
	    {
	        switch (tiffFormatCode) {
	            case 1: return INT8_U;
	            case 2: return STRING;
	            case 3: return INT16_U;
	            case 4: return INT32_U;
	            case 5: return RATIONAL_U;
	            case 6: return INT8_S;
	            case 7: return UNDEFINED;
	            case 8: return INT16_S;
	            case 9: return INT32_S;
	            case 10: return RATIONAL_S;
	            case 11: return SINGLE;
	            case 12: return DOUBLE;
	        }
	        return null;
	    }

	    private TiffDataFormat(@NotNull String name, int tiffFormatCode, int componentSizeBytes)
	    {
	        _name = name;
	        _tiffFormatCode = tiffFormatCode;
	        _componentSizeBytes = componentSizeBytes;
	    }

	    public int getComponentSizeBytes()
	    {
	        return _componentSizeBytes;
	    }

	    public int getTiffFormatCode()
	    {
	        return _tiffFormatCode;
	    }

	    @Override
	    @NotNull
	    public String toString()
	    {
	        return _name;
	    }
	}


	
	/**
	 * Processes TIFF-formatted data, calling into client code via that {@link TiffHandler} interface.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class TiffReader
	{
		
		public TiffReader() {
			
		}
	    /**
	     * Processes a TIFF data sequence.
	     *
	     * @param reader the {@link RandomAccessReader} from which the data should be read
	     * @param handler the {@link TiffHandler} that will coordinate processing and accept read values
	     * @param tiffHeaderOffset the offset within <code>reader</code> at which the TIFF header starts
	     * @throws TiffProcessingException if an error occurred during the processing of TIFF data that could not be
	     *                                 ignored or recovered from
	     * @throws IOException an error occurred while accessing the required data
	     */
	    public void processTiff(@NotNull final RandomAccessReader reader,
	                            @NotNull final TiffHandler handler,
	                            final int tiffHeaderOffset) throws TiffProcessingException, IOException
	    {
	        // This must be either "MM" or "II".
	        short byteOrderIdentifier = reader.getInt16(tiffHeaderOffset);

	        if (byteOrderIdentifier == 0x4d4d) { // "MM"
	            reader.setMotorolaByteOrder(true);
	        } else if (byteOrderIdentifier == 0x4949) { // "II"
	            reader.setMotorolaByteOrder(false);
	        } else {
	            throw new TiffProcessingException("Unclear distinction between Motorola/Intel byte ordering: " + byteOrderIdentifier);
	        }

	        // Check the next two values for correctness.
	        final int tiffMarker = reader.getUInt16(2 + tiffHeaderOffset);
	        handler.setTiffMarker(tiffMarker);

	        int firstIfdOffset = reader.getInt32(4 + tiffHeaderOffset) + tiffHeaderOffset;

	        // David Ekholm sent a digital camera image that has this problem
	        // TODO getLength should be avoided as it causes RandomAccessStreamReader to read to the end of the stream
	        if (firstIfdOffset >= reader.getLength() - 1) {
	            handler.warn("First IFD offset is beyond the end of the TIFF data segment -- trying default offset");
	            // First directory normally starts immediately after the offset bytes, so try that
	            firstIfdOffset = tiffHeaderOffset + 2 + 2 + 4;
	        }

	        Set<Integer> processedIfdOffsets = new HashSet<Integer>();
	        processIfd(handler, reader, processedIfdOffsets, firstIfdOffset, tiffHeaderOffset);
	    }

	    /**
	     * Processes a TIFF IFD.
	     *
	     * IFD Header:
	     * <ul>
	     *     <li><b>2 bytes</b> number of tags</li>
	     * </ul>
	     * Tag structure:
	     * <ul>
	     *     <li><b>2 bytes</b> tag type</li>
	     *     <li><b>2 bytes</b> format code (values 1 to 12, inclusive)</li>
	     *     <li><b>4 bytes</b> component count</li>
	     *     <li><b>4 bytes</b> inline value, or offset pointer if too large to fit in four bytes</li>
	     * </ul>
	     *
	     *
	     * @param handler the {@link com.drew.imaging.tiff.TiffHandler} that will coordinate processing and accept read values
	     * @param reader the {@link com.drew.lang.RandomAccessReader} from which the data should be read
	     * @param processedIfdOffsets the set of visited IFD offsets, to avoid revisiting the same IFD in an endless loop
	     * @param ifdOffset the offset within <code>reader</code> at which the IFD data starts
	     * @param tiffHeaderOffset the offset within <code>reader</code> at which the TIFF header starts
	     * @throws IOException an error occurred while accessing the required data
	     */
	    public void processIfd(@NotNull final TiffHandler handler,
	                                  @NotNull final RandomAccessReader reader,
	                                  @NotNull final Set<Integer> processedIfdOffsets,
	                                  final int ifdOffset,
	                                  final int tiffHeaderOffset) throws IOException
	    {
	        Boolean resetByteOrder = null;
	        try {
	            // check for directories we've already visited to avoid stack overflows when recursive/cyclic directory structures exist
	            if (processedIfdOffsets.contains(Integer.valueOf(ifdOffset))) {
	                return;
	            }

	            // remember that we've visited this directory so that we don't visit it again later
	            processedIfdOffsets.add(ifdOffset);

	            if (ifdOffset >= reader.getLength() || ifdOffset < 0) {
	                handler.error("Ignored IFD marked to start outside data segment");
	                return;
	            }

	            // First two bytes in the IFD are the number of tags in this directory
	            int dirTagCount = reader.getUInt16(ifdOffset);

	            // Some software modifies the byte order of the file, but misses some IFDs (such as makernotes).
	            // The entire test image repository doesn't contain a single IFD with more than 255 entries.
	            // Here we detect switched bytes that suggest this problem, and temporarily swap the byte order.
	            // This was discussed in GitHub issue #136.
	            if (dirTagCount > 0xFF && (dirTagCount & 0xFF) == 0) {
	                resetByteOrder = reader.isMotorolaByteOrder();
	                dirTagCount >>= 8;
	                reader.setMotorolaByteOrder(!reader.isMotorolaByteOrder());
	            }

	            int dirLength = (2 + (12 * dirTagCount) + 4);
	            if (dirLength + ifdOffset > reader.getLength()) {
	                handler.error("Illegally sized IFD");
	                return;
	            }

	            //
	            // Handle each tag in this directory
	            //
	            int invalidTiffFormatCodeCount = 0;
	            for (int tagNumber = 0; tagNumber < dirTagCount; tagNumber++) {
	                final int tagOffset = calculateTagOffset(ifdOffset, tagNumber);

	                // 2 bytes for the tag id
	                final int tagId = reader.getUInt16(tagOffset);

	                // 2 bytes for the format code
	                final int formatCode = reader.getUInt16(tagOffset + 2);
	                TiffDataFormat td = new TiffDataFormat();
	                final TiffDataFormat format = td.fromTiffFormatCode(formatCode);

	                // 4 bytes dictate the number of components in this tag's data
	                final long componentCount = reader.getUInt32(tagOffset + 4);

	                final long byteCount;
	                if (format == null) {
	                    Long byteCountOverride = handler.tryCustomProcessFormat(tagId, formatCode, componentCount);
	                    if (byteCountOverride == null) {
	                        // This error suggests that we are processing at an incorrect index and will generate
	                        // rubbish until we go out of bounds (which may be a while).  Exit now.
	                        handler.error(String.format("Invalid TIFF tag format code %d for tag 0x%04X", formatCode, tagId));
	                        // TODO specify threshold as a parameter, or provide some other external control over this behaviour
	                        if (++invalidTiffFormatCodeCount > 5) {
	                            handler.error("Stopping processing as too many errors seen in TIFF IFD");
	                            return;
	                        }
	                        continue;
	                    }
	                    byteCount = byteCountOverride;
	                } else {
	                    byteCount = componentCount * format.getComponentSizeBytes();
	                }

	                final long tagValueOffset;
	                if (byteCount > 4) {
	                    // If it's bigger than 4 bytes, the dir entry contains an offset.
	                    final long offsetVal = reader.getUInt32(tagOffset + 8);
	                    if (offsetVal + byteCount > reader.getLength()) {
	                        // Bogus pointer offset and / or byteCount value
	                        handler.error("Illegal TIFF tag pointer offset");
	                        continue;
	                    }
	                    tagValueOffset = tiffHeaderOffset + offsetVal;
	                } else {
	                    // 4 bytes or less and value is in the dir entry itself.
	                    tagValueOffset = tagOffset + 8;
	                }

	                if (tagValueOffset < 0 || tagValueOffset > reader.getLength()) {
	                    handler.error("Illegal TIFF tag pointer offset");
	                    continue;
	                }

	                // Check that this tag isn't going to allocate outside the bounds of the data array.
	                // This addresses an uncommon OutOfMemoryError.
	                if (byteCount < 0 || tagValueOffset + byteCount > reader.getLength()) {
	                    handler.error("Illegal number of bytes for TIFF tag data: " + byteCount);
	                    continue;
	                }

	                // Some tags point to one or more additional IFDs to process
	                boolean isIfdPointer = false;
	                if (byteCount == 4 * componentCount) {
	                    for (int i = 0; i < componentCount; i++) {
	                        if (handler.tryEnterSubIfd(tagId)) {
	                            isIfdPointer = true;
	                            int subDirOffset = tiffHeaderOffset + reader.getInt32((int) (tagValueOffset + i * 4));
	                            processIfd(handler, reader, processedIfdOffsets, subDirOffset, tiffHeaderOffset);
	                        }
	                    }
	                }

	                // If it wasn't an IFD pointer, allow custom tag processing to occur
	                if (!isIfdPointer && !handler.customProcessTag((int) tagValueOffset, processedIfdOffsets, tiffHeaderOffset, reader, tagId, (int) byteCount)) {
	                    // If no custom processing occurred, process the tag in the standard fashion
	                    processTag(handler, tagId, (int) tagValueOffset, (int) componentCount, formatCode, reader);
	                }
	            }

	            // at the end of each IFD is an optional link to the next IFD
	            final int finalTagOffset = calculateTagOffset(ifdOffset, dirTagCount);
	            int nextIfdOffset = reader.getInt32(finalTagOffset);
	            if (nextIfdOffset != 0) {
	                nextIfdOffset += tiffHeaderOffset;
	                if (nextIfdOffset >= reader.getLength()) {
	                    // Last 4 bytes of IFD reference another IFD with an address that is out of bounds
	                    // Note this could have been caused by jhead 1.3 cropping too much
	                    return;
	                } else if (nextIfdOffset < ifdOffset) {
	                    // TODO is this a valid restriction?
	                    // Last 4 bytes of IFD reference another IFD with an address that is before the start of this directory
	                    return;
	                }

	                if (handler.hasFollowerIfd()) {
	                    processIfd(handler, reader, processedIfdOffsets, nextIfdOffset, tiffHeaderOffset);
	                }
	            }
	        } finally {
	            handler.endingIFD();
	            if (resetByteOrder != null)
	                reader.setMotorolaByteOrder(resetByteOrder);
	        }
	    }

	    private void processTag(@NotNull final TiffHandler handler,
	                                   final int tagId,
	                                   final int tagValueOffset,
	                                   final int componentCount,
	                                   final int formatCode,
	                                   @NotNull final RandomAccessReader reader) throws IOException
	    {
	        switch (formatCode) {
	            case TiffDataFormat.CODE_UNDEFINED:
	                // this includes exif user comments
	                handler.setByteArray(tagId, reader.getBytes(tagValueOffset, componentCount));
	                break;
	            case TiffDataFormat.CODE_STRING:
	                handler.setString(tagId, reader.getNullTerminatedStringValue(tagValueOffset, componentCount, null));
	                break;
	            case TiffDataFormat.CODE_RATIONAL_S:
	                if (componentCount == 1) {
	                    handler.setRational(tagId, new Rational(reader.getInt32(tagValueOffset), reader.getInt32(tagValueOffset + 4)));
	                } else if (componentCount > 1) {
	                    Rational[] array = new Rational[componentCount];
	                    for (int i = 0; i < componentCount; i++)
	                        array[i] = new Rational(reader.getInt32(tagValueOffset + (8 * i)), reader.getInt32(tagValueOffset + 4 + (8 * i)));
	                    handler.setRationalArray(tagId, array);
	                }
	                break;
	            case TiffDataFormat.CODE_RATIONAL_U:
	                if (componentCount == 1) {
	                    handler.setRational(tagId, new Rational(reader.getUInt32(tagValueOffset), reader.getUInt32(tagValueOffset + 4)));
	                } else if (componentCount > 1) {
	                    Rational[] array = new Rational[componentCount];
	                    for (int i = 0; i < componentCount; i++)
	                        array[i] = new Rational(reader.getUInt32(tagValueOffset + (8 * i)), reader.getUInt32(tagValueOffset + 4 + (8 * i)));
	                    handler.setRationalArray(tagId, array);
	                }
	                break;
	            case TiffDataFormat.CODE_SINGLE:
	                if (componentCount == 1) {
	                    handler.setFloat(tagId, reader.getFloat32(tagValueOffset));
	                } else {
	                    float[] array = new float[componentCount];
	                    for (int i = 0; i < componentCount; i++)
	                        array[i] = reader.getFloat32(tagValueOffset + (i * 4));
	                    handler.setFloatArray(tagId, array);
	                }
	                break;
	            case TiffDataFormat.CODE_DOUBLE:
	                if (componentCount == 1) {
	                    handler.setDouble(tagId, reader.getDouble64(tagValueOffset));
	                } else {
	                    double[] array = new double[componentCount];
	                    for (int i = 0; i < componentCount; i++)
	                        array[i] = reader.getDouble64(tagValueOffset + (i * 8));
	                    handler.setDoubleArray(tagId, array);
	                }
	                break;
	            case TiffDataFormat.CODE_INT8_S:
	                if (componentCount == 1) {
	                    handler.setInt8s(tagId, reader.getInt8(tagValueOffset));
	                } else {
	                    byte[] array = new byte[componentCount];
	                    for (int i = 0; i < componentCount; i++)
	                        array[i] = reader.getInt8(tagValueOffset + i);
	                    handler.setInt8sArray(tagId, array);
	                }
	                break;
	            case TiffDataFormat.CODE_INT8_U:
	                if (componentCount == 1) {
	                    handler.setInt8u(tagId, reader.getUInt8(tagValueOffset));
	                } else {
	                    short[] array = new short[componentCount];
	                    for (int i = 0; i < componentCount; i++)
	                        array[i] = reader.getUInt8(tagValueOffset + i);
	                    handler.setInt8uArray(tagId, array);
	                }
	                break;
	            case TiffDataFormat.CODE_INT16_S:
	                if (componentCount == 1) {
	                    handler.setInt16s(tagId, (int)reader.getInt16(tagValueOffset));
	                } else {
	                    short[] array = new short[componentCount];
	                    for (int i = 0; i < componentCount; i++)
	                        array[i] = reader.getInt16(tagValueOffset + (i * 2));
	                    handler.setInt16sArray(tagId, array);
	                }
	                break;
	            case TiffDataFormat.CODE_INT16_U:
	                if (componentCount == 1) {
	                    handler.setInt16u(tagId, reader.getUInt16(tagValueOffset));
	                } else {
	                    int[] array = new int[componentCount];
	                    for (int i = 0; i < componentCount; i++)
	                        array[i] = reader.getUInt16(tagValueOffset + (i * 2));
	                    handler.setInt16uArray(tagId, array);
	                }
	                break;
	            case TiffDataFormat.CODE_INT32_S:
	                // NOTE 'long' in this case means 32 bit, not 64
	                if (componentCount == 1) {
	                    handler.setInt32s(tagId, reader.getInt32(tagValueOffset));
	                } else {
	                    int[] array = new int[componentCount];
	                    for (int i = 0; i < componentCount; i++)
	                        array[i] = reader.getInt32(tagValueOffset + (i * 4));
	                    handler.setInt32sArray(tagId, array);
	                }
	                break;
	            case TiffDataFormat.CODE_INT32_U:
	                // NOTE 'long' in this case means 32 bit, not 64
	                if (componentCount == 1) {
	                    handler.setInt32u(tagId, reader.getUInt32(tagValueOffset));
	                } else {
	                    long[] array = new long[componentCount];
	                    for (int i = 0; i < componentCount; i++)
	                        array[i] = reader.getUInt32(tagValueOffset + (i * 4));
	                    handler.setInt32uArray(tagId, array);
	                }
	                break;
	            default:
	                handler.error(String.format("Invalid TIFF tag format code %d for tag 0x%04X", formatCode, tagId));
	        }
	    }

	    /**
	     * Determine the offset of a given tag within the specified IFD.
	     *
	     * @param ifdStartOffset the offset at which the IFD starts
	     * @param entryNumber    the zero-based entry number
	     */
	    private int calculateTagOffset(int ifdStartOffset, int entryNumber)
	    {
	        // Add 2 bytes for the tag count.
	        // Each entry is 12 bytes.
	        return ifdStartOffset + 2 + (12 * entryNumber);
	    }
	}
	
	/**
	 * A directory to use for the reporting of errors. No values may be added to this directory, only warnings and errors.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */

	public final class ErrorDirectory extends Directory
	{

	    public ErrorDirectory()
	    {}

	    public ErrorDirectory(String error)
	    {
	        super.addError(error);
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Error";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return new HashMap<Integer, String>();
	    }

	    @Override
	    @NotNull
	    public String getTagName(int tagType)
	    {
	        return "";
	    }

	    @Override
	    public boolean hasTagName(int tagType)
	    {
	        return false;
	    }

	    @Override
	    public void setObject(int tagType, @NotNull Object value)
	    {
	        throw new UnsupportedOperationException(String.format("Cannot add value to %s.", ErrorDirectory.class.getName()));
	    }
	}

	
	/**
	 * Adapter between the {@link TiffHandler} interface and the {@link Metadata}/{@link Directory} object model.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public abstract class DirectoryTiffHandler implements TiffHandler
	{
	    private final Stack<Directory> _directoryStack = new Stack<Directory>();

	    @Nullable private Directory _rootParentDirectory;
	    @Nullable protected Directory _currentDirectory;
	    protected final Metadata _metadata;

	    protected DirectoryTiffHandler(Metadata metadata, @Nullable Directory parentDirectory)
	    {
	        _metadata = metadata;
	        _rootParentDirectory = parentDirectory;
	    }

	    public void endingIFD()
	    {
	        _currentDirectory = _directoryStack.empty() ? null : _directoryStack.pop();
	    }

	    protected void pushDirectory(@NotNull Class<? extends Directory> directoryClass)
	    {
	        Directory newDirectory;

	        try {
	            newDirectory = directoryClass.newInstance();
	        } catch (InstantiationException e) {
	            throw new RuntimeException(e);
	        } catch (IllegalAccessException e) {
	            throw new RuntimeException(e);
	        }

	        // If this is the first directory, don't add to the stack
	        if (_currentDirectory == null) {
	            // Apply any pending root parent to this new directory
	            if (_rootParentDirectory != null) {
	                newDirectory.setParent(_rootParentDirectory);
	                _rootParentDirectory = null;
	            }
	        }
	        else {
	            // The current directory is pushed onto the stack, and set as the new directory's parent
	            _directoryStack.push(_currentDirectory);
	            newDirectory.setParent(_currentDirectory);
	        }

	        _currentDirectory = newDirectory;
	        _metadata.addDirectory(_currentDirectory);
	    }

	    public void warn(@NotNull String message)
	    {
	        getCurrentOrErrorDirectory().addError(message);
	    }

	    public void error(@NotNull String message)
	    {
	        getCurrentOrErrorDirectory().addError(message);
	    }

	    @NotNull
	    private Directory getCurrentOrErrorDirectory()
	    {
	        if (_currentDirectory != null)
	            return _currentDirectory;
	        ErrorDirectory error = _metadata.getFirstDirectoryOfType(ErrorDirectory.class);
	        if (error != null)
	            return error;
	        pushDirectory(ErrorDirectory.class);
	        return _currentDirectory;
	    }

	    public void setByteArray(int tagId, @NotNull byte[] bytes)
	    {
	        _currentDirectory.setByteArray(tagId, bytes);
	    }

	    public void setString(int tagId, @NotNull StringValue string)
	    {
	        _currentDirectory.setStringValue(tagId, string);
	    }

	    public void setRational(int tagId, @NotNull Rational rational)
	    {
	        _currentDirectory.setRational(tagId, rational);
	    }

	    public void setRationalArray(int tagId, @NotNull Rational[] array)
	    {
	        _currentDirectory.setRationalArray(tagId, array);
	    }

	    public void setFloat(int tagId, float float32)
	    {
	        _currentDirectory.setFloat(tagId, float32);
	    }

	    public void setFloatArray(int tagId, @NotNull float[] array)
	    {
	        _currentDirectory.setFloatArray(tagId, array);
	    }

	    public void setDouble(int tagId, double double64)
	    {
	        _currentDirectory.setDouble(tagId, double64);
	    }

	    public void setDoubleArray(int tagId, @NotNull double[] array)
	    {
	        _currentDirectory.setDoubleArray(tagId, array);
	    }

	    public void setInt8s(int tagId, byte int8s)
	    {
	        // NOTE Directory stores all integral types as int32s, except for int32u and long
	        _currentDirectory.setInt(tagId, int8s);
	    }

	    public void setInt8sArray(int tagId, @NotNull byte[] array)
	    {
	        // NOTE Directory stores all integral types as int32s, except for int32u and long
	        _currentDirectory.setByteArray(tagId, array);
	    }

	    public void setInt8u(int tagId, short int8u)
	    {
	        // NOTE Directory stores all integral types as int32s, except for int32u and long
	        _currentDirectory.setInt(tagId, int8u);
	    }

	    public void setInt8uArray(int tagId, @NotNull short[] array)
	    {
	        // TODO create and use a proper setter for short[]
	        _currentDirectory.setObjectArray(tagId, array);
	    }

	    public void setInt16s(int tagId, int int16s)
	    {
	        // TODO create and use a proper setter for int16u?
	        _currentDirectory.setInt(tagId, int16s);
	    }

	    public void setInt16sArray(int tagId, @NotNull short[] array)
	    {
	        // TODO create and use a proper setter for short[]
	        _currentDirectory.setObjectArray(tagId, array);
	    }

	    public void setInt16u(int tagId, int int16u)
	    {
	        // TODO create and use a proper setter for
	        _currentDirectory.setInt(tagId, int16u);
	    }

	    public void setInt16uArray(int tagId, @NotNull int[] array)
	    {
	        // TODO create and use a proper setter for short[]
	        _currentDirectory.setObjectArray(tagId, array);
	    }

	    public void setInt32s(int tagId, int int32s)
	    {
	        _currentDirectory.setInt(tagId, int32s);
	    }

	    public void setInt32sArray(int tagId, @NotNull int[] array)
	    {
	        _currentDirectory.setIntArray(tagId, array);
	    }

	    public void setInt32u(int tagId, long int32u)
	    {
	        _currentDirectory.setLong(tagId, int32u);
	    }

	    public void setInt32uArray(int tagId, @NotNull long[] array)
	    {
	        // TODO create and use a proper setter for short[]
	        _currentDirectory.setObjectArray(tagId, array);
	    }
	}
	
	/**
	 * Contains helper methods that perform photographic conversions.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public final static class PhotographicConversions
	{
	    public final static double ROOT_TWO = Math.sqrt(2);

	    private PhotographicConversions() throws Exception
	    {
	        throw new Exception("Not intended for instantiation.");
	    }

	    /**
	     * Converts an aperture value to its corresponding F-stop number.
	     *
	     * @param aperture the aperture value to convert
	     * @return the F-stop number of the specified aperture
	     */
	    public static double apertureToFStop(double aperture)
	    {
	        return Math.pow(ROOT_TWO, aperture);

	        // NOTE jhead uses a different calculation as far as i can tell...  this confuses me...
	        // fStop = (float)Math.exp(aperture * Math.log(2) * 0.5));
	    }

	    /**
	     * Converts a shutter speed to an exposure time.
	     *
	     * @param shutterSpeed the shutter speed to convert
	     * @return the exposure time of the specified shutter speed
	     */
	    public static double shutterSpeedToExposureTime(double shutterSpeed)
	    {
	        return (float) (1 / Math.exp(shutterSpeed * Math.log(2)));
	    }
	}

	
	/**
	 * Base class for several Exif format tag directories.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public abstract class ExifDirectoryBase extends Directory
	{
	    public static final int TAG_INTEROP_INDEX = 0x0001;
	    public static final int TAG_INTEROP_VERSION = 0x0002;

	    /**
	     * The new subfile type tag.
	     * 0 = Full-resolution Image
	     * 1 = Reduced-resolution image
	     * 2 = Single page of multi-page image
	     * 3 = Single page of multi-page reduced-resolution image
	     * 4 = Transparency mask
	     * 5 = Transparency mask of reduced-resolution image
	     * 6 = Transparency mask of multi-page image
	     * 7 = Transparency mask of reduced-resolution multi-page image
	     */
	    public static final int TAG_NEW_SUBFILE_TYPE                  = 0x00FE;
	    /**
	     * The old subfile type tag.
	     * 1 = Full-resolution image (Main image)
	     * 2 = Reduced-resolution image (Thumbnail)
	     * 3 = Single page of multi-page image
	     */
	    public static final int TAG_SUBFILE_TYPE                      = 0x00FF;

	    public static final int TAG_IMAGE_WIDTH                       = 0x0100;
	    public static final int TAG_IMAGE_HEIGHT                      = 0x0101;

	    /**
	     * When image format is no compression, this value shows the number of bits
	     * per component for each pixel. Usually this value is '8,8,8'.
	     */
	    public static final int TAG_BITS_PER_SAMPLE                   = 0x0102;
	    public static final int TAG_COMPRESSION                       = 0x0103;

	    /**
	     * Shows the color space of the image data components.
	     * 0 = WhiteIsZero
	     * 1 = BlackIsZero
	     * 2 = RGB
	     * 3 = RGB Palette
	     * 4 = Transparency Mask
	     * 5 = CMYK
	     * 6 = YCbCr
	     * 8 = CIELab
	     * 9 = ICCLab
	     * 10 = ITULab
	     * 32803 = Color Filter Array
	     * 32844 = Pixar LogL
	     * 32845 = Pixar LogLuv
	     * 34892 = Linear Raw
	     */
	    public static final int TAG_PHOTOMETRIC_INTERPRETATION        = 0x0106;

	    /**
	     * 1 = No dithering or halftoning
	     * 2 = Ordered dither or halftone
	     * 3 = Randomized dither
	     */
	    public static final int TAG_THRESHOLDING                      = 0x0107;

	    /**
	     * 1 = Normal
	     * 2 = Reversed
	     */
	    public static final int TAG_FILL_ORDER                        = 0x010A;
	    public static final int TAG_DOCUMENT_NAME                     = 0x010D;

	    public static final int TAG_IMAGE_DESCRIPTION                 = 0x010E;

	    public static final int TAG_MAKE                              = 0x010F;
	    public static final int TAG_MODEL                             = 0x0110;
	    /** The position in the file of raster data. */
	    public static final int TAG_STRIP_OFFSETS                     = 0x0111;
	    public static final int TAG_ORIENTATION                       = 0x0112;
	    /** Each pixel is composed of this many samples. */
	    public static final int TAG_SAMPLES_PER_PIXEL                 = 0x0115;
	    /** The raster is codified by a single block of data holding this many rows. */
	    public static final int TAG_ROWS_PER_STRIP                    = 0x0116;
	    /** The size of the raster data in bytes. */
	    public static final int TAG_STRIP_BYTE_COUNTS                 = 0x0117;
	    public static final int TAG_MIN_SAMPLE_VALUE                  = 0x0118;
	    public static final int TAG_MAX_SAMPLE_VALUE                  = 0x0119;
	    public static final int TAG_X_RESOLUTION                      = 0x011A;
	    public static final int TAG_Y_RESOLUTION                      = 0x011B;
	    /**
	     * When image format is no compression YCbCr, this value shows byte aligns of
	     * YCbCr data. If value is '1', Y/Cb/Cr value is chunky format, contiguous for
	     * each subsampling pixel. If value is '2', Y/Cb/Cr value is separated and
	     * stored to Y plane/Cb plane/Cr plane format.
	     */
	    public static final int TAG_PLANAR_CONFIGURATION              = 0x011C;
	    public static final int TAG_PAGE_NAME                         = 0x011D;

	    public static final int TAG_RESOLUTION_UNIT                   = 0x0128;
	    public static final int TAG_PAGE_NUMBER                       = 0x0129;

	    public static final int TAG_TRANSFER_FUNCTION                 = 0x012D;
	    public static final int TAG_SOFTWARE                          = 0x0131;
	    public static final int TAG_DATETIME                          = 0x0132;
	    public static final int TAG_ARTIST                            = 0x013B;
	    public static final int TAG_HOST_COMPUTER                     = 0x013C;
	    public static final int TAG_PREDICTOR                         = 0x013D;
	    public static final int TAG_WHITE_POINT                       = 0x013E;
	    public static final int TAG_PRIMARY_CHROMATICITIES            = 0x013F;

	    public static final int TAG_TILE_WIDTH                        = 0x0142;
	    public static final int TAG_TILE_LENGTH                       = 0x0143;
	    public static final int TAG_TILE_OFFSETS                      = 0x0144;
	    public static final int TAG_TILE_BYTE_COUNTS                  = 0x0145;

	    /**
	     * Tag is a pointer to one or more sub-IFDs.
	     + Seems to be used exclusively by raw formats, referencing one or two IFDs.
	     */
	    public static final int TAG_SUB_IFD_OFFSET                    = 0x014a;

	    public static final int TAG_EXTRA_SAMPLES                     = 0x0152;
	    public static final int TAG_SAMPLE_FORMAT                     = 0x0153;

	    public static final int TAG_TRANSFER_RANGE                    = 0x0156;
	    public static final int TAG_JPEG_TABLES                       = 0x015B;
	    public static final int TAG_JPEG_PROC                         = 0x0200;

	    // 0x0201 can have all kinds of descriptions for thumbnail starting index
	    // 0x0202 can have all kinds of descriptions for thumbnail length
	    public static final int TAG_JPEG_RESTART_INTERVAL = 0x0203;
	    public static final int TAG_JPEG_LOSSLESS_PREDICTORS = 0x0205;
	    public static final int TAG_JPEG_POINT_TRANSFORMS = 0x0206;
	    public static final int TAG_JPEG_Q_TABLES = 0x0207;
	    public static final int TAG_JPEG_DC_TABLES = 0x0208;
	    public static final int TAG_JPEG_AC_TABLES = 0x0209;

	    public static final int TAG_YCBCR_COEFFICIENTS                = 0x0211;
	    public static final int TAG_YCBCR_SUBSAMPLING                 = 0x0212;
	    public static final int TAG_YCBCR_POSITIONING                 = 0x0213;
	    public static final int TAG_REFERENCE_BLACK_WHITE             = 0x0214;
	    public static final int TAG_STRIP_ROW_COUNTS                  = 0x022f;
	    public static final int TAG_APPLICATION_NOTES                 = 0x02bc;

	    public static final int TAG_RELATED_IMAGE_FILE_FORMAT         = 0x1000;
	    public static final int TAG_RELATED_IMAGE_WIDTH               = 0x1001;
	    public static final int TAG_RELATED_IMAGE_HEIGHT              = 0x1002;

	    public static final int TAG_RATING                            = 0x4746;
	    public static final int TAG_RATING_PERCENT                    = 0x4749;

	    public static final int TAG_CFA_REPEAT_PATTERN_DIM            = 0x828D;
	    /** There are two definitions for CFA pattern, I don't know the difference... */
	    public static final int TAG_CFA_PATTERN_2                     = 0x828E;
	    public static final int TAG_BATTERY_LEVEL                     = 0x828F;
	    public static final int TAG_COPYRIGHT                         = 0x8298;
	    /**
	     * Exposure time (reciprocal of shutter speed). Unit is second.
	     */
	    public static final int TAG_EXPOSURE_TIME                     = 0x829A;
	    /**
	     * The actual F-number(F-stop) of lens when the image was taken.
	     */
	    public static final int TAG_FNUMBER                           = 0x829D;
	    public static final int TAG_IPTC_NAA                          = 0x83BB;
	    public static final int TAG_PHOTOSHOP_SETTINGS                = 0x8649;
	    public static final int TAG_INTER_COLOR_PROFILE               = 0x8773;
	    /**
	     * Exposure program that the camera used when image was taken. '1' means
	     * manual control, '2' program normal, '3' aperture priority, '4' shutter
	     * priority, '5' program creative (slow program), '6' program action
	     * (high-speed program), '7' portrait mode, '8' landscape mode.
	     */
	    public static final int TAG_EXPOSURE_PROGRAM                  = 0x8822;
	    public static final int TAG_SPECTRAL_SENSITIVITY              = 0x8824;
	    public static final int TAG_ISO_EQUIVALENT                    = 0x8827;
	    /**
	     * Indicates the Opto-Electric Conversion Function (OECF) specified in ISO 14524.
	     * <p>
	     * OECF is the relationship between the camera optical input and the image values.
	     * <p>
	     * The values are:
	     * <ul>
	     *   <li>Two shorts, indicating respectively number of columns, and number of rows.</li>
	     *   <li>For each column, the column name in a null-terminated ASCII string.</li>
	     *   <li>For each cell, an SRATIONAL value.</li>
	     * </ul>
	     */
	    public static final int TAG_OPTO_ELECTRIC_CONVERSION_FUNCTION = 0x8828;
	    public static final int TAG_INTERLACE                         = 0x8829;
	    public static final int TAG_TIME_ZONE_OFFSET_TIFF_EP          = 0x882A;
	    public static final int TAG_SELF_TIMER_MODE_TIFF_EP           = 0x882B;
	    /**
	     * Applies to ISO tag.
	     *
	     * 0 = Unknown
	     * 1 = Standard Output Sensitivity
	     * 2 = Recommended Exposure Index
	     * 3 = ISO Speed
	     * 4 = Standard Output Sensitivity and Recommended Exposure Index
	     * 5 = Standard Output Sensitivity and ISO Speed
	     * 6 = Recommended Exposure Index and ISO Speed
	     * 7 = Standard Output Sensitivity, Recommended Exposure Index and ISO Speed
	     */
	    public static final int TAG_SENSITIVITY_TYPE                  = 0x8830;
	    public static final int TAG_STANDARD_OUTPUT_SENSITIVITY       = 0x8831;
	    public static final int TAG_RECOMMENDED_EXPOSURE_INDEX        = 0x8832;
	    public static final int TAG_ISO_SPEED                         = 0x8833;
	    public static final int TAG_ISO_SPEED_LATITUDE_YYY            = 0x8834;
	    public static final int TAG_ISO_SPEED_LATITUDE_ZZZ            = 0x8835;

	    public static final int TAG_EXIF_VERSION                      = 0x9000;
	    public static final int TAG_DATETIME_ORIGINAL                 = 0x9003;
	    public static final int TAG_DATETIME_DIGITIZED                = 0x9004;
	    public static final int TAG_TIME_ZONE                         = 0x9010;
	    public static final int TAG_TIME_ZONE_ORIGINAL                = 0x9011;
	    public static final int TAG_TIME_ZONE_DIGITIZED               = 0x9012;

	    public static final int TAG_COMPONENTS_CONFIGURATION          = 0x9101;
	    /**
	     * Average (rough estimate) compression level in JPEG bits per pixel.
	     * */
	    public static final int TAG_COMPRESSED_AVERAGE_BITS_PER_PIXEL = 0x9102;

	    /**
	     * Shutter speed by APEX value. To convert this value to ordinary 'Shutter Speed';
	     * calculate this value's power of 2, then reciprocal. For example, if the
	     * ShutterSpeedValue is '4', shutter speed is 1/(24)=1/16 second.
	     */
	    public static final int TAG_SHUTTER_SPEED                     = 0x9201;
	    /**
	     * The actual aperture value of lens when the image was taken. Unit is APEX.
	     * To convert this value to ordinary F-number (F-stop), calculate this value's
	     * power of root 2 (=1.4142). For example, if the ApertureValue is '5',
	     * F-number is 1.4142^5 = F5.6.
	     */
	    public static final int TAG_APERTURE                          = 0x9202;
	    public static final int TAG_BRIGHTNESS_VALUE                  = 0x9203;
	    public static final int TAG_EXPOSURE_BIAS                     = 0x9204;
	    /**
	     * Maximum aperture value of lens. You can convert to F-number by calculating
	     * power of root 2 (same process of ApertureValue:0x9202).
	     * The actual aperture value of lens when the image was taken. To convert this
	     * value to ordinary f-number(f-stop), calculate the value's power of root 2
	     * (=1.4142). For example, if the ApertureValue is '5', f-number is 1.41425^5 = F5.6.
	     */
	    public static final int TAG_MAX_APERTURE                      = 0x9205;
	    /**
	     * Indicates the distance the autofocus camera is focused to.  Tends to be less accurate as distance increases.
	     */
	    public static final int TAG_SUBJECT_DISTANCE                  = 0x9206;
	    /**
	     * Exposure metering method. '0' means unknown, '1' average, '2' center
	     * weighted average, '3' spot, '4' multi-spot, '5' multi-segment, '6' partial,
	     * '255' other.
	     */
	    public static final int TAG_METERING_MODE                     = 0x9207;

	    /**
	     * @deprecated use {@link com.drew.metadata.exif.ExifDirectoryBase#TAG_WHITE_BALANCE} instead.
	     */
	    @Deprecated
	    public static final int TAG_LIGHT_SOURCE                      = 0x9208;
	    /**
	     * White balance (aka light source). '0' means unknown, '1' daylight,
	     * '2' fluorescent, '3' tungsten, '10' flash, '17' standard light A,
	     * '18' standard light B, '19' standard light C, '20' D55, '21' D65,
	     * '22' D75, '255' other.
	     */
	    public static final int TAG_WHITE_BALANCE                     = 0x9208;
	    /**
	     * 0x0  = 0000000 = No Flash
	     * 0x1  = 0000001 = Fired
	     * 0x5  = 0000101 = Fired, Return not detected
	     * 0x7  = 0000111 = Fired, Return detected
	     * 0x9  = 0001001 = On
	     * 0xd  = 0001101 = On, Return not detected
	     * 0xf  = 0001111 = On, Return detected
	     * 0x10 = 0010000 = Off
	     * 0x18 = 0011000 = Auto, Did not fire
	     * 0x19 = 0011001 = Auto, Fired
	     * 0x1d = 0011101 = Auto, Fired, Return not detected
	     * 0x1f = 0011111 = Auto, Fired, Return detected
	     * 0x20 = 0100000 = No flash function
	     * 0x41 = 1000001 = Fired, Red-eye reduction
	     * 0x45 = 1000101 = Fired, Red-eye reduction, Return not detected
	     * 0x47 = 1000111 = Fired, Red-eye reduction, Return detected
	     * 0x49 = 1001001 = On, Red-eye reduction
	     * 0x4d = 1001101 = On, Red-eye reduction, Return not detected
	     * 0x4f = 1001111 = On, Red-eye reduction, Return detected
	     * 0x59 = 1011001 = Auto, Fired, Red-eye reduction
	     * 0x5d = 1011101 = Auto, Fired, Red-eye reduction, Return not detected
	     * 0x5f = 1011111 = Auto, Fired, Red-eye reduction, Return detected
	     *        6543210 (positions)
	     *
	     * This is a bitmask.
	     * 0 = flash fired
	     * 1 = return detected
	     * 2 = return able to be detected
	     * 3 = unknown
	     * 4 = auto used
	     * 5 = unknown
	     * 6 = red eye reduction used
	     */
	    public static final int TAG_FLASH                             = 0x9209;
	    /**
	     * Focal length of lens used to take image.  Unit is millimeter.
	     * Nice digital cameras actually save the focal length as a function of how far they are zoomed in.
	     */
	    public static final int TAG_FOCAL_LENGTH                      = 0x920A;

	    public static final int TAG_FLASH_ENERGY_TIFF_EP              = 0x920B;
	    public static final int TAG_SPATIAL_FREQ_RESPONSE_TIFF_EP     = 0x920C;
	    public static final int TAG_NOISE                             = 0x920D;
	    public static final int TAG_FOCAL_PLANE_X_RESOLUTION_TIFF_EP  = 0x920E;
	    public static final int TAG_FOCAL_PLANE_Y_RESOLUTION_TIFF_EP = 0x920F;
	    public static final int TAG_IMAGE_NUMBER                      = 0x9211;
	    public static final int TAG_SECURITY_CLASSIFICATION           = 0x9212;
	    public static final int TAG_IMAGE_HISTORY                     = 0x9213;
	    public static final int TAG_SUBJECT_LOCATION_TIFF_EP          = 0x9214;
	    public static final int TAG_EXPOSURE_INDEX_TIFF_EP            = 0x9215;
	    public static final int TAG_STANDARD_ID_TIFF_EP               = 0x9216;

	    /**
	     * This tag holds the Exif Makernote. Makernotes are free to be in any format, though they are often IFDs.
	     * To determine the format, we consider the starting bytes of the makernote itself and sometimes the
	     * camera model and make.
	     * <p>
	     * The component count for this tag includes all of the bytes needed for the makernote.
	     */
	    public static final int TAG_MAKERNOTE                         = 0x927C;

	    public static final int TAG_USER_COMMENT                      = 0x9286;

	    public static final int TAG_SUBSECOND_TIME                    = 0x9290;
	    public static final int TAG_SUBSECOND_TIME_ORIGINAL           = 0x9291;
	    public static final int TAG_SUBSECOND_TIME_DIGITIZED          = 0x9292;

	    public static final int TAG_TEMPERATURE                       = 0x9400;
	    public static final int TAG_HUMIDITY                          = 0x9401;
	    public static final int TAG_PRESSURE                          = 0x9402;
	    public static final int TAG_WATER_DEPTH                       = 0x9403;
	    public static final int TAG_ACCELERATION                      = 0x9404;
	    public static final int TAG_CAMERA_ELEVATION_ANGLE            = 0x9405;

	    /** The image title, as used by Windows XP. */
	    public static final int TAG_WIN_TITLE                         = 0x9C9B;
	    /** The image comment, as used by Windows XP. */
	    public static final int TAG_WIN_COMMENT                       = 0x9C9C;
	    /** The image author, as used by Windows XP (called Artist in the Windows shell). */
	    public static final int TAG_WIN_AUTHOR                        = 0x9C9D;
	    /** The image keywords, as used by Windows XP. */
	    public static final int TAG_WIN_KEYWORDS                      = 0x9C9E;
	    /** The image subject, as used by Windows XP. */
	    public static final int TAG_WIN_SUBJECT                       = 0x9C9F;

	    public static final int TAG_FLASHPIX_VERSION                  = 0xA000;
	    /**
	     * Defines Color Space. DCF image must use sRGB color space so value is
	     * always '1'. If the picture uses the other color space, value is
	     * '65535':Uncalibrated.
	     */
	    public static final int TAG_COLOR_SPACE                       = 0xA001;
	    public static final int TAG_EXIF_IMAGE_WIDTH                  = 0xA002;
	    public static final int TAG_EXIF_IMAGE_HEIGHT                 = 0xA003;
	    public static final int TAG_RELATED_SOUND_FILE                = 0xA004;

	    public static final int TAG_FLASH_ENERGY                      = 0xA20B;
	    public static final int TAG_SPATIAL_FREQ_RESPONSE             = 0xA20C;
	    public static final int TAG_FOCAL_PLANE_X_RESOLUTION          = 0xA20E;
	    public static final int TAG_FOCAL_PLANE_Y_RESOLUTION          = 0xA20F;
	    /**
	     * Unit of FocalPlaneXResolution/FocalPlaneYResolution. '1' means no-unit,
	     * '2' inch, '3' centimeter.
	     *
	     * Note: Some of Fujifilm's digicam(e.g.FX2700,FX2900,Finepix4700Z/40i etc)
	     * uses value '3' so it must be 'centimeter', but it seems that they use a
	     * '8.3mm?'(1/3in.?) to their ResolutionUnit. Fuji's BUG? Finepix4900Z has
	     * been changed to use value '2' but it doesn't match to actual value also.
	     */
	    public static final int TAG_FOCAL_PLANE_RESOLUTION_UNIT       = 0xA210;
	    public static final int TAG_SUBJECT_LOCATION                  = 0xA214;
	    public static final int TAG_EXPOSURE_INDEX                    = 0xA215;
	    public static final int TAG_SENSING_METHOD                    = 0xA217;

	    public static final int TAG_FILE_SOURCE                       = 0xA300;
	    public static final int TAG_SCENE_TYPE                        = 0xA301;
	    public static final int TAG_CFA_PATTERN                       = 0xA302;

	    /**
	     * This tag indicates the use of special processing on image data, such as rendering
	     * geared to output. When special processing is performed, the reader is expected to
	     * disable or minimize any further processing.
	     * Tag = 41985 (A401.H)
	     * Type = SHORT
	     * Count = 1
	     * Default = 0
	     *   0 = Normal process
	     *   1 = Custom process
	     *   Other = reserved
	     */
	    public static final int TAG_CUSTOM_RENDERED                   = 0xA401;
	    /**
	     * This tag indicates the exposure mode set when the image was shot. In auto-bracketing
	     * mode, the camera shoots a series of frames of the same scene at different exposure settings.
	     * Tag = 41986 (A402.H)
	     * Type = SHORT
	     * Count = 1
	     * Default = none
	     *   0 = Auto exposure
	     *   1 = Manual exposure
	     *   2 = Auto bracket
	     *   Other = reserved
	     */
	    public static final int TAG_EXPOSURE_MODE                     = 0xA402;
	    /**
	     * This tag indicates the white balance mode set when the image was shot.
	     * Tag = 41987 (A403.H)
	     * Type = SHORT
	     * Count = 1
	     * Default = none
	     *   0 = Auto white balance
	     *   1 = Manual white balance
	     *   Other = reserved
	     */
	    public static final int TAG_WHITE_BALANCE_MODE                = 0xA403;
	    /**
	     * This tag indicates the digital zoom ratio when the image was shot. If the
	     * numerator of the recorded value is 0, this indicates that digital zoom was
	     * not used.
	     * Tag = 41988 (A404.H)
	     * Type = RATIONAL
	     * Count = 1
	     * Default = none
	     */
	    public static final int TAG_DIGITAL_ZOOM_RATIO                = 0xA404;
	    /**
	     * This tag indicates the equivalent focal length assuming a 35mm film camera,
	     * in mm. A value of 0 means the focal length is unknown. Note that this tag
	     * differs from the FocalLength tag.
	     * Tag = 41989 (A405.H)
	     * Type = SHORT
	     * Count = 1
	     * Default = none
	     */
	    public static final int TAG_35MM_FILM_EQUIV_FOCAL_LENGTH      = 0xA405;
	    /**
	     * This tag indicates the type of scene that was shot. It can also be used to
	     * record the mode in which the image was shot. Note that this differs from
	     * the scene type (SceneType) tag.
	     * Tag = 41990 (A406.H)
	     * Type = SHORT
	     * Count = 1
	     * Default = 0
	     *   0 = Standard
	     *   1 = Landscape
	     *   2 = Portrait
	     *   3 = Night scene
	     *   Other = reserved
	     */
	    public static final int TAG_SCENE_CAPTURE_TYPE                = 0xA406;
	    /**
	     * This tag indicates the degree of overall image gain adjustment.
	     * Tag = 41991 (A407.H)
	     * Type = SHORT
	     * Count = 1
	     * Default = none
	     *   0 = None
	     *   1 = Low gain up
	     *   2 = High gain up
	     *   3 = Low gain down
	     *   4 = High gain down
	     *   Other = reserved
	     */
	    public static final int TAG_GAIN_CONTROL                      = 0xA407;
	    /**
	     * This tag indicates the direction of contrast processing applied by the camera
	     * when the image was shot.
	     * Tag = 41992 (A408.H)
	     * Type = SHORT
	     * Count = 1
	     * Default = 0
	     *   0 = Normal
	     *   1 = Soft
	     *   2 = Hard
	     *   Other = reserved
	     */
	    public static final int TAG_CONTRAST                          = 0xA408;
	    /**
	     * This tag indicates the direction of saturation processing applied by the camera
	     * when the image was shot.
	     * Tag = 41993 (A409.H)
	     * Type = SHORT
	     * Count = 1
	     * Default = 0
	     *   0 = Normal
	     *   1 = Low saturation
	     *   2 = High saturation
	     *   Other = reserved
	     */
	    public static final int TAG_SATURATION                        = 0xA409;
	    /**
	     * This tag indicates the direction of sharpness processing applied by the camera
	     * when the image was shot.
	     * Tag = 41994 (A40A.H)
	     * Type = SHORT
	     * Count = 1
	     * Default = 0
	     *   0 = Normal
	     *   1 = Soft
	     *   2 = Hard
	     *   Other = reserved
	     */
	    public static final int TAG_SHARPNESS                         = 0xA40A;
	    /**
	     * This tag indicates information on the picture-taking conditions of a particular
	     * camera model. The tag is used only to indicate the picture-taking conditions in
	     * the reader.
	     * Tag = 41995 (A40B.H)
	     * Type = UNDEFINED
	     * Count = Any
	     * Default = none
	     *
	     * The information is recorded in the format shown below. The data is recorded
	     * in Unicode using SHORT type for the number of display rows and columns and
	     * UNDEFINED type for the camera settings. The Unicode (UCS-2) string including
	     * Signature is NULL terminated. The specifics of the Unicode string are as given
	     * in ISO/IEC 10464-1.
	     *
	     *      Length  Type        Meaning
	     *      ------+-----------+------------------
	     *      2       SHORT       Display columns
	     *      2       SHORT       Display rows
	     *      Any     UNDEFINED   Camera setting-1
	     *      Any     UNDEFINED   Camera setting-2
	     *      :       :           :
	     *      Any     UNDEFINED   Camera setting-n
	     */
	    public static final int TAG_DEVICE_SETTING_DESCRIPTION        = 0xA40B;
	    /**
	     * This tag indicates the distance to the subject.
	     * Tag = 41996 (A40C.H)
	     * Type = SHORT
	     * Count = 1
	     * Default = none
	     *   0 = unknown
	     *   1 = Macro
	     *   2 = Close view
	     *   3 = Distant view
	     *   Other = reserved
	     */
	    public static final int TAG_SUBJECT_DISTANCE_RANGE            = 0xA40C;

	    /**
	     * This tag indicates an identifier assigned uniquely to each image. It is
	     * recorded as an ASCII string equivalent to hexadecimal notation and 128-bit
	     * fixed length.
	     * Tag = 42016 (A420.H)
	     * Type = ASCII
	     * Count = 33
	     * Default = none
	     */
	    public static final int TAG_IMAGE_UNIQUE_ID                   = 0xA420;
	    /** String. */
	    public static final int TAG_CAMERA_OWNER_NAME                 = 0xA430;
	    /** String. */
	    public static final int TAG_BODY_SERIAL_NUMBER                = 0xA431;
	    /** An array of four Rational64u numbers giving focal and aperture ranges. */
	    public static final int TAG_LENS_SPECIFICATION                = 0xA432;
	    /** String. */
	    public static final int TAG_LENS_MAKE                         = 0xA433;
	    /** String. */
	    public static final int TAG_LENS_MODEL                        = 0xA434;
	    /** String. */
	    public static final int TAG_LENS_SERIAL_NUMBER                = 0xA435;
	    /** Rational64u. */
	    public static final int TAG_GAMMA                             = 0xA500;

	    public static final int TAG_PRINT_IMAGE_MATCHING_INFO         = 0xC4A5;

	    public static final int TAG_PANASONIC_TITLE                   = 0xC6D2;
	    public static final int TAG_PANASONIC_TITLE_2                 = 0xC6D3;

	    public static final int TAG_PADDING                           = 0xEA1C;

	    public static final int TAG_LENS                              = 0xFDEA;

	    protected void addExifTagNames(HashMap<Integer, String> map)
	    {
	        map.put(TAG_INTEROP_INDEX, "Interoperability Index");
	        map.put(TAG_INTEROP_VERSION, "Interoperability Version");
	        map.put(TAG_NEW_SUBFILE_TYPE, "New Subfile Type");
	        map.put(TAG_SUBFILE_TYPE, "Subfile Type");
	        map.put(TAG_IMAGE_WIDTH, "Image Width");
	        map.put(TAG_IMAGE_HEIGHT, "Image Height");
	        map.put(TAG_BITS_PER_SAMPLE, "Bits Per Sample");
	        map.put(TAG_COMPRESSION, "Compression");
	        map.put(TAG_PHOTOMETRIC_INTERPRETATION, "Photometric Interpretation");
	        map.put(TAG_THRESHOLDING, "Thresholding");
	        map.put(TAG_FILL_ORDER, "Fill Order");
	        map.put(TAG_DOCUMENT_NAME, "Document Name");
	        map.put(TAG_IMAGE_DESCRIPTION, "Image Description");
	        map.put(TAG_MAKE, "Make");
	        map.put(TAG_MODEL, "Model");
	        map.put(TAG_STRIP_OFFSETS, "Strip Offsets");
	        map.put(TAG_ORIENTATION, "Orientation");
	        map.put(TAG_SAMPLES_PER_PIXEL, "Samples Per Pixel");
	        map.put(TAG_ROWS_PER_STRIP, "Rows Per Strip");
	        map.put(TAG_STRIP_BYTE_COUNTS, "Strip Byte Counts");
	        map.put(TAG_MIN_SAMPLE_VALUE, "Minimum Sample Value");
	        map.put(TAG_MAX_SAMPLE_VALUE, "Maximum Sample Value");
	        map.put(TAG_X_RESOLUTION, "X Resolution");
	        map.put(TAG_Y_RESOLUTION, "Y Resolution");
	        map.put(TAG_PLANAR_CONFIGURATION, "Planar Configuration");
	        map.put(TAG_PAGE_NAME, "Page Name");
	        map.put(TAG_RESOLUTION_UNIT, "Resolution Unit");
	        map.put(TAG_PAGE_NUMBER, "Page Number");
	        map.put(TAG_TRANSFER_FUNCTION, "Transfer Function");
	        map.put(TAG_SOFTWARE, "Software");
	        map.put(TAG_DATETIME, "Date/Time");
	        map.put(TAG_ARTIST, "Artist");
	        map.put(TAG_PREDICTOR, "Predictor");
	        map.put(TAG_HOST_COMPUTER, "Host Computer");
	        map.put(TAG_WHITE_POINT, "White Point");
	        map.put(TAG_PRIMARY_CHROMATICITIES, "Primary Chromaticities");
	        map.put(TAG_TILE_WIDTH, "Tile Width");
	        map.put(TAG_TILE_LENGTH, "Tile Length");
	        map.put(TAG_TILE_OFFSETS, "Tile Offsets");
	        map.put(TAG_TILE_BYTE_COUNTS, "Tile Byte Counts");
	        map.put(TAG_SUB_IFD_OFFSET, "Sub IFD Pointer(s)");
	        map.put(TAG_EXTRA_SAMPLES, "Extra Samples");
	        map.put(TAG_SAMPLE_FORMAT, "Sample Format");
	        map.put(TAG_TRANSFER_RANGE, "Transfer Range");
	        map.put(TAG_JPEG_TABLES, "JPEG Tables");
	        map.put(TAG_JPEG_PROC, "JPEG Proc");

	        map.put(TAG_JPEG_RESTART_INTERVAL, "JPEG Restart Interval");
	        map.put(TAG_JPEG_LOSSLESS_PREDICTORS, "JPEG Lossless Predictors");
	        map.put(TAG_JPEG_POINT_TRANSFORMS, "JPEG Point Transforms");
	        map.put(TAG_JPEG_Q_TABLES, "JPEGQ Tables");
	        map.put(TAG_JPEG_DC_TABLES, "JPEGDC Tables");
	        map.put(TAG_JPEG_AC_TABLES, "JPEGAC Tables");

	        map.put(TAG_YCBCR_COEFFICIENTS, "YCbCr Coefficients");
	        map.put(TAG_YCBCR_SUBSAMPLING, "YCbCr Sub-Sampling");
	        map.put(TAG_YCBCR_POSITIONING, "YCbCr Positioning");
	        map.put(TAG_REFERENCE_BLACK_WHITE, "Reference Black/White");
	        map.put(TAG_STRIP_ROW_COUNTS, "Strip Row Counts");
	        map.put(TAG_APPLICATION_NOTES, "Application Notes");
	        map.put(TAG_RELATED_IMAGE_FILE_FORMAT, "Related Image File Format");
	        map.put(TAG_RELATED_IMAGE_WIDTH, "Related Image Width");
	        map.put(TAG_RELATED_IMAGE_HEIGHT, "Related Image Height");
	        map.put(TAG_RATING, "Rating");
	        map.put(TAG_RATING_PERCENT, "Rating Percent");
	        map.put(TAG_CFA_REPEAT_PATTERN_DIM, "CFA Repeat Pattern Dim");
	        map.put(TAG_CFA_PATTERN_2, "CFA Pattern");
	        map.put(TAG_BATTERY_LEVEL, "Battery Level");
	        map.put(TAG_COPYRIGHT, "Copyright");
	        map.put(TAG_EXPOSURE_TIME, "Exposure Time");
	        map.put(TAG_FNUMBER, "F-Number");
	        map.put(TAG_IPTC_NAA, "IPTC/NAA");
	        map.put(TAG_PHOTOSHOP_SETTINGS, "Photoshop Settings");
	        map.put(TAG_INTER_COLOR_PROFILE, "Inter Color Profile");
	        map.put(TAG_EXPOSURE_PROGRAM, "Exposure Program");
	        map.put(TAG_SPECTRAL_SENSITIVITY, "Spectral Sensitivity");
	        map.put(TAG_ISO_EQUIVALENT, "ISO Speed Ratings");
	        map.put(TAG_OPTO_ELECTRIC_CONVERSION_FUNCTION, "Opto-electric Conversion Function (OECF)");
	        map.put(TAG_INTERLACE, "Interlace");
	        map.put(TAG_TIME_ZONE_OFFSET_TIFF_EP, "Time Zone Offset");
	        map.put(TAG_SELF_TIMER_MODE_TIFF_EP, "Self Timer Mode");
	        map.put(TAG_SENSITIVITY_TYPE, "Sensitivity Type");
	        map.put(TAG_STANDARD_OUTPUT_SENSITIVITY, "Standard Output Sensitivity");
	        map.put(TAG_RECOMMENDED_EXPOSURE_INDEX, "Recommended Exposure Index");
	        map.put(TAG_ISO_SPEED, "ISO Speed");
	        map.put(TAG_ISO_SPEED_LATITUDE_YYY, "ISO Speed Latitude yyy");
	        map.put(TAG_ISO_SPEED_LATITUDE_ZZZ, "ISO Speed Latitude zzz");
	        map.put(TAG_EXIF_VERSION, "Exif Version");
	        map.put(TAG_DATETIME_ORIGINAL, "Date/Time Original");
	        map.put(TAG_DATETIME_DIGITIZED, "Date/Time Digitized");
	        map.put(TAG_TIME_ZONE, "Time Zone");
	        map.put(TAG_TIME_ZONE_ORIGINAL, "Time Zone Original");
	        map.put(TAG_TIME_ZONE_DIGITIZED, "Time Zone Digitized");
	        map.put(TAG_COMPONENTS_CONFIGURATION, "Components Configuration");
	        map.put(TAG_COMPRESSED_AVERAGE_BITS_PER_PIXEL, "Compressed Bits Per Pixel");
	        map.put(TAG_SHUTTER_SPEED, "Shutter Speed Value");
	        map.put(TAG_APERTURE, "Aperture Value");
	        map.put(TAG_BRIGHTNESS_VALUE, "Brightness Value");
	        map.put(TAG_EXPOSURE_BIAS, "Exposure Bias Value");
	        map.put(TAG_MAX_APERTURE, "Max Aperture Value");
	        map.put(TAG_SUBJECT_DISTANCE, "Subject Distance");
	        map.put(TAG_METERING_MODE, "Metering Mode");
	        map.put(TAG_WHITE_BALANCE, "White Balance");
	        map.put(TAG_FLASH, "Flash");
	        map.put(TAG_FOCAL_LENGTH, "Focal Length");
	        map.put(TAG_FLASH_ENERGY_TIFF_EP, "Flash Energy");
	        map.put(TAG_SPATIAL_FREQ_RESPONSE_TIFF_EP, "Spatial Frequency Response");
	        map.put(TAG_NOISE, "Noise");
	        map.put(TAG_FOCAL_PLANE_X_RESOLUTION_TIFF_EP, "Focal Plane X Resolution");
	        map.put(TAG_FOCAL_PLANE_Y_RESOLUTION_TIFF_EP, "Focal Plane Y Resolution");
	        map.put(TAG_IMAGE_NUMBER, "Image Number");
	        map.put(TAG_SECURITY_CLASSIFICATION, "Security Classification");
	        map.put(TAG_IMAGE_HISTORY, "Image History");
	        map.put(TAG_SUBJECT_LOCATION_TIFF_EP, "Subject Location");
	        map.put(TAG_EXPOSURE_INDEX_TIFF_EP, "Exposure Index");
	        map.put(TAG_STANDARD_ID_TIFF_EP, "TIFF/EP Standard ID");
	        map.put(TAG_MAKERNOTE, "Makernote");
	        map.put(TAG_USER_COMMENT, "User Comment");
	        map.put(TAG_SUBSECOND_TIME, "Sub-Sec Time");
	        map.put(TAG_SUBSECOND_TIME_ORIGINAL, "Sub-Sec Time Original");
	        map.put(TAG_SUBSECOND_TIME_DIGITIZED, "Sub-Sec Time Digitized");
	        map.put(TAG_TEMPERATURE, "Temperature");
	        map.put(TAG_HUMIDITY, "Humidity");
	        map.put(TAG_PRESSURE, "Pressure");
	        map.put(TAG_WATER_DEPTH, "Water Depth");
	        map.put(TAG_ACCELERATION, "Acceleration");
	        map.put(TAG_CAMERA_ELEVATION_ANGLE, "Camera Elevation Angle");
	        map.put(TAG_WIN_TITLE, "Windows XP Title");
	        map.put(TAG_WIN_COMMENT, "Windows XP Comment");
	        map.put(TAG_WIN_AUTHOR, "Windows XP Author");
	        map.put(TAG_WIN_KEYWORDS, "Windows XP Keywords");
	        map.put(TAG_WIN_SUBJECT, "Windows XP Subject");
	        map.put(TAG_FLASHPIX_VERSION, "FlashPix Version");
	        map.put(TAG_COLOR_SPACE, "Color Space");
	        map.put(TAG_EXIF_IMAGE_WIDTH, "Exif Image Width");
	        map.put(TAG_EXIF_IMAGE_HEIGHT, "Exif Image Height");
	        map.put(TAG_RELATED_SOUND_FILE, "Related Sound File");
	        map.put(TAG_FLASH_ENERGY, "Flash Energy");
	        map.put(TAG_SPATIAL_FREQ_RESPONSE, "Spatial Frequency Response");
	        map.put(TAG_FOCAL_PLANE_X_RESOLUTION, "Focal Plane X Resolution");
	        map.put(TAG_FOCAL_PLANE_Y_RESOLUTION, "Focal Plane Y Resolution");
	        map.put(TAG_FOCAL_PLANE_RESOLUTION_UNIT, "Focal Plane Resolution Unit");
	        map.put(TAG_SUBJECT_LOCATION, "Subject Location");
	        map.put(TAG_EXPOSURE_INDEX, "Exposure Index");
	        map.put(TAG_SENSING_METHOD, "Sensing Method");
	        map.put(TAG_FILE_SOURCE, "File Source");
	        map.put(TAG_SCENE_TYPE, "Scene Type");
	        map.put(TAG_CFA_PATTERN, "CFA Pattern");
	        map.put(TAG_CUSTOM_RENDERED, "Custom Rendered");
	        map.put(TAG_EXPOSURE_MODE, "Exposure Mode");
	        map.put(TAG_WHITE_BALANCE_MODE, "White Balance Mode");
	        map.put(TAG_DIGITAL_ZOOM_RATIO, "Digital Zoom Ratio");
	        map.put(TAG_35MM_FILM_EQUIV_FOCAL_LENGTH, "Focal Length 35");
	        map.put(TAG_SCENE_CAPTURE_TYPE, "Scene Capture Type");
	        map.put(TAG_GAIN_CONTROL, "Gain Control");
	        map.put(TAG_CONTRAST, "Contrast");
	        map.put(TAG_SATURATION, "Saturation");
	        map.put(TAG_SHARPNESS, "Sharpness");
	        map.put(TAG_DEVICE_SETTING_DESCRIPTION, "Device Setting Description");
	        map.put(TAG_SUBJECT_DISTANCE_RANGE, "Subject Distance Range");
	        map.put(TAG_IMAGE_UNIQUE_ID, "Unique Image ID");
	        map.put(TAG_CAMERA_OWNER_NAME, "Camera Owner Name");
	        map.put(TAG_BODY_SERIAL_NUMBER, "Body Serial Number");
	        map.put(TAG_LENS_SPECIFICATION, "Lens Specification");
	        map.put(TAG_LENS_MAKE, "Lens Make");
	        map.put(TAG_LENS_MODEL, "Lens Model");
	        map.put(TAG_LENS_SERIAL_NUMBER, "Lens Serial Number");
	        map.put(TAG_GAMMA, "Gamma");
	        map.put(TAG_PRINT_IMAGE_MATCHING_INFO, "Print Image Matching (PIM) Info");
	        map.put(TAG_PANASONIC_TITLE, "Panasonic Title");
	        map.put(TAG_PANASONIC_TITLE_2, "Panasonic Title (2)");
	        map.put(TAG_PADDING, "Padding");
	        map.put(TAG_LENS, "Lens");
	    }
	}
	
	/**
	 * Base class for several Exif format descriptor classes.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifDescriptorBase<T extends Directory> extends TagDescriptor<T>
	{
	    /**
	     * Dictates whether rational values will be represented in decimal format in instances
	     * where decimal notation is elegant (such as 1/2 -> 0.5, but not 1/3).
	     */
	    private final boolean _allowDecimalRepresentationOfRationals = true;

	    // Note for the potential addition of brightness presentation in eV:
	    // Brightness of taken subject. To calculate Exposure(Ev) from BrightnessValue(Bv),
	    // you must add SensitivityValue(Sv).
	    // Ev=BV+Sv   Sv=log2(ISOSpeedRating/3.125)
	    // ISO100:Sv=5, ISO200:Sv=6, ISO400:Sv=7, ISO125:Sv=5.32.

	    public ExifDescriptorBase(@NotNull T directory)
	    {
	        super(directory);
	    }

	    @Nullable
	    @Override
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case ExifDirectoryBase.TAG_INTEROP_INDEX:
	                return getInteropIndexDescription();
	            case ExifDirectoryBase.TAG_INTEROP_VERSION:
	                return getInteropVersionDescription();
	            case ExifDirectoryBase.TAG_NEW_SUBFILE_TYPE:
	                return getNewSubfileTypeDescription();
	            case ExifDirectoryBase.TAG_SUBFILE_TYPE:
	                return getSubfileTypeDescription();
	            case ExifDirectoryBase.TAG_IMAGE_WIDTH:
	                return getImageWidthDescription();
	            case ExifDirectoryBase.TAG_IMAGE_HEIGHT:
	                return getImageHeightDescription();
	            case ExifDirectoryBase.TAG_BITS_PER_SAMPLE:
	                return getBitsPerSampleDescription();
	            case ExifDirectoryBase.TAG_COMPRESSION:
	                return getCompressionDescription();
	            case ExifDirectoryBase.TAG_PHOTOMETRIC_INTERPRETATION:
	                return getPhotometricInterpretationDescription();
	            case ExifDirectoryBase.TAG_THRESHOLDING:
	                return getThresholdingDescription();
	            case ExifDirectoryBase.TAG_FILL_ORDER:
	                return getFillOrderDescription();
	            case ExifDirectoryBase.TAG_ORIENTATION:
	                return getOrientationDescription();
	            case ExifDirectoryBase.TAG_SAMPLES_PER_PIXEL:
	                return getSamplesPerPixelDescription();
	            case ExifDirectoryBase.TAG_ROWS_PER_STRIP:
	                return getRowsPerStripDescription();
	            case ExifDirectoryBase.TAG_STRIP_BYTE_COUNTS:
	                return getStripByteCountsDescription();
	            case ExifDirectoryBase.TAG_X_RESOLUTION:
	                return getXResolutionDescription();
	            case ExifDirectoryBase.TAG_Y_RESOLUTION:
	                return getYResolutionDescription();
	            case ExifDirectoryBase.TAG_PLANAR_CONFIGURATION:
	                return getPlanarConfigurationDescription();
	            case ExifDirectoryBase.TAG_RESOLUTION_UNIT:
	                return getResolutionDescription();
	            case ExifDirectoryBase.TAG_JPEG_PROC:
	                return getJpegProcDescription();
	            case ExifDirectoryBase.TAG_YCBCR_SUBSAMPLING:
	                return getYCbCrSubsamplingDescription();
	            case ExifDirectoryBase.TAG_YCBCR_POSITIONING:
	                return getYCbCrPositioningDescription();
	            case ExifDirectoryBase.TAG_REFERENCE_BLACK_WHITE:
	                return getReferenceBlackWhiteDescription();
	            case ExifDirectoryBase.TAG_CFA_PATTERN_2:
	                return getCfaPattern2Description();
	            case ExifDirectoryBase.TAG_EXPOSURE_TIME:
	                return getExposureTimeDescription();
	            case ExifDirectoryBase.TAG_FNUMBER:
	                return getFNumberDescription();
	            case ExifDirectoryBase.TAG_EXPOSURE_PROGRAM:
	                return getExposureProgramDescription();
	            case ExifDirectoryBase.TAG_ISO_EQUIVALENT:
	                return getIsoEquivalentDescription();
	            case ExifDirectoryBase.TAG_SENSITIVITY_TYPE:
	                return getSensitivityTypeRangeDescription();
	            case ExifDirectoryBase.TAG_EXIF_VERSION:
	                return getExifVersionDescription();
	            case ExifDirectoryBase.TAG_COMPONENTS_CONFIGURATION:
	                return getComponentConfigurationDescription();
	            case ExifDirectoryBase.TAG_COMPRESSED_AVERAGE_BITS_PER_PIXEL:
	                return getCompressedAverageBitsPerPixelDescription();
	            case ExifDirectoryBase.TAG_SHUTTER_SPEED:
	                return getShutterSpeedDescription();
	            case ExifDirectoryBase.TAG_APERTURE:
	                return getApertureValueDescription();
	            case ExifDirectoryBase.TAG_BRIGHTNESS_VALUE:
	                return getBrightnessValueDescription();
	            case ExifDirectoryBase.TAG_EXPOSURE_BIAS:
	                return getExposureBiasDescription();
	            case ExifDirectoryBase.TAG_MAX_APERTURE:
	                return getMaxApertureValueDescription();
	            case ExifDirectoryBase.TAG_SUBJECT_DISTANCE:
	                return getSubjectDistanceDescription();
	            case ExifDirectoryBase.TAG_METERING_MODE:
	                return getMeteringModeDescription();
	            case ExifDirectoryBase.TAG_WHITE_BALANCE:
	                return getWhiteBalanceDescription();
	            case ExifDirectoryBase.TAG_FLASH:
	                return getFlashDescription();
	            case ExifDirectoryBase.TAG_FOCAL_LENGTH:
	                return getFocalLengthDescription();
	            case ExifDirectoryBase.TAG_USER_COMMENT:
	                return getUserCommentDescription();
	            case ExifDirectoryBase.TAG_TEMPERATURE:
	                return getTemperatureDescription();
	            case ExifDirectoryBase.TAG_HUMIDITY:
	                return getHumidityDescription();
	            case ExifDirectoryBase.TAG_PRESSURE:
	                return getPressureDescription();
	            case ExifDirectoryBase.TAG_WATER_DEPTH:
	                return getWaterDepthDescription();
	            case ExifDirectoryBase.TAG_ACCELERATION:
	                return getAccelerationDescription();
	            case ExifDirectoryBase.TAG_CAMERA_ELEVATION_ANGLE:
	                return getCameraElevationAngleDescription();
	            case ExifDirectoryBase.TAG_WIN_TITLE:
	                return getWindowsTitleDescription();
	            case ExifDirectoryBase.TAG_WIN_COMMENT:
	                return getWindowsCommentDescription();
	            case ExifDirectoryBase.TAG_WIN_AUTHOR:
	                return getWindowsAuthorDescription();
	            case ExifDirectoryBase.TAG_WIN_KEYWORDS:
	                return getWindowsKeywordsDescription();
	            case ExifDirectoryBase.TAG_WIN_SUBJECT:
	                return getWindowsSubjectDescription();
	            case ExifDirectoryBase.TAG_FLASHPIX_VERSION:
	                return getFlashPixVersionDescription();
	            case ExifDirectoryBase.TAG_COLOR_SPACE:
	                return getColorSpaceDescription();
	            case ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH:
	                return getExifImageWidthDescription();
	            case ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT:
	                return getExifImageHeightDescription();
	            case ExifDirectoryBase.TAG_FOCAL_PLANE_X_RESOLUTION:
	                return getFocalPlaneXResolutionDescription();
	            case ExifDirectoryBase.TAG_FOCAL_PLANE_Y_RESOLUTION:
	                return getFocalPlaneYResolutionDescription();
	            case ExifDirectoryBase.TAG_FOCAL_PLANE_RESOLUTION_UNIT:
	                return getFocalPlaneResolutionUnitDescription();
	            case ExifDirectoryBase.TAG_SENSING_METHOD:
	                return getSensingMethodDescription();
	            case ExifDirectoryBase.TAG_FILE_SOURCE:
	                return getFileSourceDescription();
	            case ExifDirectoryBase.TAG_SCENE_TYPE:
	                return getSceneTypeDescription();
	            case ExifDirectoryBase.TAG_CFA_PATTERN:
	                return getCfaPatternDescription();
	            case ExifDirectoryBase.TAG_CUSTOM_RENDERED:
	                return getCustomRenderedDescription();
	            case ExifDirectoryBase.TAG_EXPOSURE_MODE:
	                return getExposureModeDescription();
	            case ExifDirectoryBase.TAG_WHITE_BALANCE_MODE:
	                return getWhiteBalanceModeDescription();
	            case ExifDirectoryBase.TAG_DIGITAL_ZOOM_RATIO:
	                return getDigitalZoomRatioDescription();
	            case ExifDirectoryBase.TAG_35MM_FILM_EQUIV_FOCAL_LENGTH:
	                return get35mmFilmEquivFocalLengthDescription();
	            case ExifDirectoryBase.TAG_SCENE_CAPTURE_TYPE:
	                return getSceneCaptureTypeDescription();
	            case ExifDirectoryBase.TAG_GAIN_CONTROL:
	                return getGainControlDescription();
	            case ExifDirectoryBase.TAG_CONTRAST:
	                return getContrastDescription();
	            case ExifDirectoryBase.TAG_SATURATION:
	                return getSaturationDescription();
	            case ExifDirectoryBase.TAG_SHARPNESS:
	                return getSharpnessDescription();
	            case ExifDirectoryBase.TAG_SUBJECT_DISTANCE_RANGE:
	                return getSubjectDistanceRangeDescription();
	            case ExifDirectoryBase.TAG_LENS_SPECIFICATION:
	                return getLensSpecificationDescription();
	            case ExifDirectoryBase.TAG_EXTRA_SAMPLES:
	                return getExtraSamplesDescription();
	            case ExifDirectoryBase.TAG_SAMPLE_FORMAT:
	                return getSampleFormatDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getInteropIndexDescription()
	    {
	        String value = _directory.getString(ExifDirectoryBase.TAG_INTEROP_INDEX);

	        if (value == null)
	            return null;

	        return "R98".equalsIgnoreCase(value.trim())
	            ? "Recommended Exif Interoperability Rules (ExifR98)"
	            : "Unknown (" + value + ")";
	    }

	    @Nullable
	    public String getInteropVersionDescription()
	    {
	        return getVersionBytesDescription(ExifDirectoryBase.TAG_INTEROP_VERSION, 2);
	    }

	    @Nullable
	    public String getNewSubfileTypeDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_NEW_SUBFILE_TYPE, 0,
	            "Full-resolution image",
	            "Reduced-resolution image",
	            "Single page of multi-page image",
	            "Single page of multi-page reduced-resolution image",
	            "Transparency mask",
	            "Transparency mask of reduced-resolution image",
	            "Transparency mask of multi-page image",
	            "Transparency mask of reduced-resolution multi-page image"
	        );
	    }

	    @Nullable
	    public String getSubfileTypeDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_SUBFILE_TYPE, 1,
	            "Full-resolution image",
	            "Reduced-resolution image",
	            "Single page of multi-page image"
	        );
	    }

	    @Nullable
	    public String getImageWidthDescription()
	    {
	        String value = _directory.getString(ExifDirectoryBase.TAG_IMAGE_WIDTH);
	        return value == null ? null : value + " pixels";
	    }

	    @Nullable
	    public String getImageHeightDescription()
	    {
	        String value = _directory.getString(ExifDirectoryBase.TAG_IMAGE_HEIGHT);
	        return value == null ? null : value + " pixels";
	    }

	    @Nullable
	    public String getBitsPerSampleDescription()
	    {
	        String value = _directory.getString(ExifDirectoryBase.TAG_BITS_PER_SAMPLE);
	        return value == null ? null : value + " bits/component/pixel";
	    }

	    @Nullable
	    public String getCompressionDescription()
	    {
	        Integer value = _directory.getInteger(ExifDirectoryBase.TAG_COMPRESSION);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 1: return "Uncompressed";
	            case 2: return "CCITT 1D";
	            case 3: return "T4/Group 3 Fax";
	            case 4: return "T6/Group 4 Fax";
	            case 5: return "LZW";
	            case 6: return "JPEG (old-style)";
	            case 7: return "JPEG";
	            case 8: return "Adobe Deflate";
	            case 9: return "JBIG B&W";
	            case 10: return "JBIG Color";
	            case 99: return "JPEG";
	            case 262: return "Kodak 262";
	            case 32766: return "Next";
	            case 32767: return "Sony ARW Compressed";
	            case 32769: return "Packed RAW";
	            case 32770: return "Samsung SRW Compressed";
	            case 32771: return "CCIRLEW";
	            case 32772: return "Samsung SRW Compressed 2";
	            case 32773: return "PackBits";
	            case 32809: return "Thunderscan";
	            case 32867: return "Kodak KDC Compressed";
	            case 32895: return "IT8CTPAD";
	            case 32896: return "IT8LW";
	            case 32897: return "IT8MP";
	            case 32898: return "IT8BL";
	            case 32908: return "PixarFilm";
	            case 32909: return "PixarLog";
	            case 32946: return "Deflate";
	            case 32947: return "DCS";
	            case 34661: return "JBIG";
	            case 34676: return "SGILog";
	            case 34677: return "SGILog24";
	            case 34712: return "JPEG 2000";
	            case 34713: return "Nikon NEF Compressed";
	            case 34715: return "JBIG2 TIFF FX";
	            case 34718: return "Microsoft Document Imaging (MDI) Binary Level Codec";
	            case 34719: return "Microsoft Document Imaging (MDI) Progressive Transform Codec";
	            case 34720: return "Microsoft Document Imaging (MDI) Vector";
	            case 34892: return "Lossy JPEG";
	            case 65000: return "Kodak DCR Compressed";
	            case 65535: return "Pentax PEF Compressed";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getPhotometricInterpretationDescription()
	    {
	        // Shows the color space of the image data components
	        Integer value = _directory.getInteger(ExifDirectoryBase.TAG_PHOTOMETRIC_INTERPRETATION);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "WhiteIsZero";
	            case 1: return "BlackIsZero";
	            case 2: return "RGB";
	            case 3: return "RGB Palette";
	            case 4: return "Transparency Mask";
	            case 5: return "CMYK";
	            case 6: return "YCbCr";
	            case 8: return "CIELab";
	            case 9: return "ICCLab";
	            case 10: return "ITULab";
	            case 32803: return "Color Filter Array";
	            case 32844: return "Pixar LogL";
	            case 32845: return "Pixar LogLuv";
	            case 32892: return "Linear Raw";
	            default:
	                return "Unknown colour space";
	        }
	    }

	    @Nullable
	    public String getThresholdingDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_THRESHOLDING, 1,
	            "No dithering or halftoning",
	            "Ordered dither or halftone",
	            "Randomized dither"
	        );
	    }

	    @Nullable
	    public String getFillOrderDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_FILL_ORDER, 1,
	            "Normal",
	            "Reversed"
	        );
	    }

	    @Nullable
	    public String getOrientationDescription()
	    {
	        return super.getOrientationDescription(ExifDirectoryBase.TAG_ORIENTATION);
	    }

	    @Nullable
	    public String getSamplesPerPixelDescription()
	    {
	        String value = _directory.getString(ExifDirectoryBase.TAG_SAMPLES_PER_PIXEL);
	        return value == null ? null : value + " samples/pixel";
	    }

	    @Nullable
	    public String getRowsPerStripDescription()
	    {
	        final String value = _directory.getString(ExifDirectoryBase.TAG_ROWS_PER_STRIP);
	        return value == null ? null : value + " rows/strip";
	    }

	    @Nullable
	    public String getStripByteCountsDescription()
	    {
	        final String value = _directory.getString(ExifDirectoryBase.TAG_STRIP_BYTE_COUNTS);
	        return value == null ? null : value + " bytes";
	    }

	    @Nullable
	    public String getXResolutionDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_X_RESOLUTION);
	        if (value == null)
	            return null;
	        final String unit = getResolutionDescription();
	        return String.format("%s dots per %s",
	            value.toSimpleString(_allowDecimalRepresentationOfRationals),
	            unit == null ? "unit" : unit.toLowerCase());
	    }

	    @Nullable
	    public String getYResolutionDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_Y_RESOLUTION);
	        if (value==null)
	            return null;
	        final String unit = getResolutionDescription();
	        return String.format("%s dots per %s",
	            value.toSimpleString(_allowDecimalRepresentationOfRationals),
	            unit == null ? "unit" : unit.toLowerCase());
	    }

	    @Nullable
	    public String getPlanarConfigurationDescription()
	    {
	        // When image format is no compression YCbCr, this value shows byte aligns of YCbCr
	        // data. If value is '1', Y/Cb/Cr value is chunky format, contiguous for each subsampling
	        // pixel. If value is '2', Y/Cb/Cr value is separated and stored to Y plane/Cb plane/Cr
	        // plane format.
	        return getIndexedDescription(ExifDirectoryBase.TAG_PLANAR_CONFIGURATION,
	            1,
	            "Chunky (contiguous for each subsampling pixel)",
	            "Separate (Y-plane/Cb-plane/Cr-plane format)"
	        );
	    }

	    @Nullable
	    public String getResolutionDescription()
	    {
	        // '1' means no-unit, '2' means inch, '3' means centimeter. Default value is '2'(inch)
	        return getIndexedDescription(ExifDirectoryBase.TAG_RESOLUTION_UNIT, 1, "(No unit)", "Inch", "cm");
	    }

	    @Nullable
	    public String getJpegProcDescription()
	    {
	        Integer value = _directory.getInteger(ExifDirectoryBase.TAG_JPEG_PROC);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 1: return "Baseline";
	            case 14: return "Lossless";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getYCbCrSubsamplingDescription()
	    {
	        int[] positions = _directory.getIntArray(ExifDirectoryBase.TAG_YCBCR_SUBSAMPLING);
	        if (positions == null || positions.length < 2)
	            return null;
	        if (positions[0] == 2 && positions[1] == 1) {
	            return "YCbCr4:2:2";
	        } else if (positions[0] == 2 && positions[1] == 2) {
	            return "YCbCr4:2:0";
	        } else {
	            return "(Unknown)";
	        }
	    }

	    @Nullable
	    public String getYCbCrPositioningDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_YCBCR_POSITIONING, 1, "Center of pixel array", "Datum point");
	    }

	    @Nullable
	    public String getReferenceBlackWhiteDescription()
	    {
	        // For some reason, sometimes this is read as a long[] and
	        // getIntArray isn't able to deal with it
	        int[] ints = _directory.getIntArray(ExifDirectoryBase.TAG_REFERENCE_BLACK_WHITE);
	        if (ints==null || ints.length < 6)
	        {
	            Object o = _directory.getObject(ExifDirectoryBase.TAG_REFERENCE_BLACK_WHITE);
	            if (o != null && (o instanceof long[]))
	            {
	                long[] longs = (long[])o;
	                if (longs.length < 6)
	                    return null;

	                ints = new int[longs.length];
	                for (int i = 0; i < longs.length; i++)
	                    ints[i] = (int)longs[i];
	            }
	            else
	                return null;
	        }

	        int blackR = ints[0];
	        int whiteR = ints[1];
	        int blackG = ints[2];
	        int whiteG = ints[3];
	        int blackB = ints[4];
	        int whiteB = ints[5];
	        return String.format("[%d,%d,%d] [%d,%d,%d]", blackR, blackG, blackB, whiteR, whiteG, whiteB);
	    }

	    /**
	     * String description of CFA Pattern
	     *
	     * Indicates the color filter array (CFA) geometric pattern of the image sensor when a one-chip color area sensor is used.
	     * It does not apply to all sensing methods.
	     *
	     * ExifDirectoryBase.TAG_CFA_PATTERN_2 holds only the pixel pattern. ExifDirectoryBase.TAG_CFA_REPEAT_PATTERN_DIM is expected to exist and pass
	     * some conditional tests.
	     */
	    @Nullable
	    public String getCfaPattern2Description()
	    {
	        byte[] values = _directory.getByteArray(ExifDirectoryBase.TAG_CFA_PATTERN_2);
	        if (values == null)
	            return null;

	        int[] repeatPattern = _directory.getIntArray(ExifDirectoryBase.TAG_CFA_REPEAT_PATTERN_DIM);
	        if (repeatPattern == null)
	            return String.format("Repeat Pattern not found for CFAPattern (%s)", super.getDescription(ExifDirectoryBase.TAG_CFA_PATTERN_2));

	        if (repeatPattern.length == 2 && values.length == (repeatPattern[0] * repeatPattern[1]))
	        {
	            int[] intpattern = new int[2 + values.length];
	            intpattern[0] = repeatPattern[0];
	            intpattern[1] = repeatPattern[1];

	            for (int i = 0; i < values.length; i++)
	                intpattern[i + 2] = values[i] & 0xFF;   // convert the values[i] byte to unsigned

	            return formatCFAPattern(intpattern);
	        }

	        return String.format("Unknown Pattern (%s)", super.getDescription(ExifDirectoryBase.TAG_CFA_PATTERN_2));
	    }

	    @Nullable
	    private String formatCFAPattern(@Nullable int[] pattern)
	    {
	        if (pattern == null)
	            return null;
	        if (pattern.length < 2)
	            return "<truncated data>";
	        if (pattern[0] == 0 && pattern[1] == 0)
	            return "<zero pattern size>";

	        int end = 2 + pattern[0] * pattern[1];
	        if (end > pattern.length)
	            return "<invalid pattern size>";

	        String[] cfaColors = { "Red", "Green", "Blue", "Cyan", "Magenta", "Yellow", "White" };

	        StringBuilder ret = new StringBuilder();
	        ret.append("[");
	        for (int pos = 2; pos < end; pos++)
	        {
	            if (pattern[pos] <= cfaColors.length - 1)
	                ret.append(cfaColors[pattern[pos]]);
	            else
	                ret.append("Unknown");      // indicated pattern position is outside the array bounds

	            if ((pos - 2) % pattern[1] == 0)
	                ret.append(",");
	            else if(pos != end - 1)
	                ret.append("][");
	        }
	        ret.append("]");

	        return ret.toString();
	    }

	    @Nullable
	    public String getExposureTimeDescription()
	    {
	        String value = _directory.getString(ExifDirectoryBase.TAG_EXPOSURE_TIME);
	        return value == null ? null : value + " sec";
	    }

	    @Nullable
	    public String getFNumberDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_FNUMBER);
	        if (value == null)
	            return null;
	        return getFStopDescription(value.doubleValue());
	    }

	    @Nullable
	    public String getExposureProgramDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_EXPOSURE_PROGRAM,
	            1,
	            "Manual control",
	            "Program normal",
	            "Aperture priority",
	            "Shutter priority",
	            "Program creative (slow program)",
	            "Program action (high-speed program)",
	            "Portrait mode",
	            "Landscape mode"
	        );
	    }

	    @Nullable
	    public String getIsoEquivalentDescription()
	    {
	        // Have seen an exception here from files produced by ACDSEE that stored an int[] here with two values
	        Integer isoEquiv = _directory.getInteger(ExifDirectoryBase.TAG_ISO_EQUIVALENT);
	        // There used to be a check here that multiplied ISO values < 50 by 200.
	        // Issue 36 shows a smart-phone image from a Samsung Galaxy S2 with ISO-40.
	        return isoEquiv != null
	            ? Integer.toString(isoEquiv)
	            : null;
	    }

	    @Nullable
	    public String getSensitivityTypeRangeDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_SENSITIVITY_TYPE,
	            "Unknown",
	            "Standard Output Sensitivity",
	            "Recommended Exposure Index",
	            "ISO Speed",
	            "Standard Output Sensitivity and Recommended Exposure Index",
	            "Standard Output Sensitivity and ISO Speed",
	            "Recommended Exposure Index and ISO Speed",
	            "Standard Output Sensitivity, Recommended Exposure Index and ISO Speed"
	        );
	    }

	    @Nullable
	    public String getExifVersionDescription()
	    {
	        return getVersionBytesDescription(ExifDirectoryBase.TAG_EXIF_VERSION, 2);
	    }

	    @Nullable
	    public String getComponentConfigurationDescription()
	    {
	        int[] components = _directory.getIntArray(ExifDirectoryBase.TAG_COMPONENTS_CONFIGURATION);
	        if (components == null)
	            return null;
	        String[] componentStrings = {"", "Y", "Cb", "Cr", "R", "G", "B"};
	        StringBuilder componentConfig = new StringBuilder();
	        for (int i = 0; i < Math.min(4, components.length); i++) {
	            int j = components[i];
	            if (j > 0 && j < componentStrings.length) {
	                componentConfig.append(componentStrings[j]);
	            }
	        }
	        return componentConfig.toString();
	    }

	    @Nullable
	    public String getCompressedAverageBitsPerPixelDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_COMPRESSED_AVERAGE_BITS_PER_PIXEL);
	        if (value == null)
	            return null;
	        String ratio = value.toSimpleString(_allowDecimalRepresentationOfRationals);
	        return value.isInteger() && value.intValue() == 1
	            ? ratio + " bit/pixel"
	            : ratio + " bits/pixel";
	    }

	    @Nullable
	    public String getShutterSpeedDescription()
	    {
	        return super.getShutterSpeedDescription(ExifDirectoryBase.TAG_SHUTTER_SPEED);
	    }

	    @Nullable
	    public String getApertureValueDescription()
	    {
	        Double aperture = _directory.getDoubleObject(ExifDirectoryBase.TAG_APERTURE);
	        if (aperture == null)
	            return null;
	        double fStop = PhotographicConversions.apertureToFStop(aperture);
	        return getFStopDescription(fStop);
	    }

	    @Nullable
	    public String getBrightnessValueDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_BRIGHTNESS_VALUE);
	        if (value == null)
	            return null;
	        if (value.getNumerator() == 0xFFFFFFFFL)
	            return "Unknown";
	        DecimalFormat formatter = new DecimalFormat("0.0##");
	        return formatter.format(value.doubleValue());
	    }

	    @Nullable
	    public String getExposureBiasDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_EXPOSURE_BIAS);
	        if (value == null)
	            return null;
	        return value.toSimpleString(true) + " EV";
	    }

	    @Nullable
	    public String getMaxApertureValueDescription()
	    {
	        Double aperture = _directory.getDoubleObject(ExifDirectoryBase.TAG_MAX_APERTURE);
	        if (aperture == null)
	            return null;
	        double fStop = PhotographicConversions.apertureToFStop(aperture);
	        return getFStopDescription(fStop);
	    }

	    @Nullable
	    public String getSubjectDistanceDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_SUBJECT_DISTANCE);
	        if (value == null)
	            return null;
	        if (value.getNumerator() == 0xFFFFFFFFL)
	            return "Infinity";
	        if (value.getNumerator() == 0)
	            return "Unknown";
	        DecimalFormat formatter = new DecimalFormat("0.0##");
	        return formatter.format(value.doubleValue()) + " metres";
	    }

	    @Nullable
	    public String getMeteringModeDescription()
	    {
	        // '0' means unknown, '1' average, '2' center weighted average, '3' spot
	        // '4' multi-spot, '5' multi-segment, '6' partial, '255' other
	        Integer value = _directory.getInteger(ExifDirectoryBase.TAG_METERING_MODE);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Unknown";
	            case 1: return "Average";
	            case 2: return "Center weighted average";
	            case 3: return "Spot";
	            case 4: return "Multi-spot";
	            case 5: return "Multi-segment";
	            case 6: return "Partial";
	            case 255: return "(Other)";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getWhiteBalanceDescription()
	    {
	        final Integer value = _directory.getInteger(ExifDirectoryBase.TAG_WHITE_BALANCE);
	        if (value == null)
	            return null;
	        return getWhiteBalanceDescription(value);
	    }

	    public String getWhiteBalanceDescription(int value)
	    {
	        // See http://web.archive.org/web/20131018091152/http://exif.org/Exif2-2.PDF page 35

	        switch (value) {
	            case 0: return "Unknown";
	            case 1: return "Daylight";
	            case 2: return "Florescent";
	            case 3: return "Tungsten (Incandescent)";
	            case 4: return "Flash";
	            case 9: return "Fine Weather";
	            case 10: return "Cloudy";
	            case 11: return "Shade";
	            case 12: return "Daylight Fluorescent";   // (D 5700 - 7100K)
	            case 13: return "Day White Fluorescent";  // (N 4600 - 5500K)
	            case 14: return "Cool White Fluorescent"; // (W 3800 - 4500K)
	            case 15: return "White Fluorescent";      // (WW 3250 - 3800K)
	            case 16: return "Warm White Fluorescent"; // (L 2600 - 3250K)
	            case 17: return "Standard light A";
	            case 18: return "Standard light B";
	            case 19: return "Standard light C";
	            case 20: return "D55";
	            case 21: return "D65";
	            case 22: return "D75";
	            case 23: return "D50";
	            case 24: return "ISO Studio Tungsten";
	            case 255: return "Other";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getFlashDescription()
	    {
	        /*
	         * This is a bit mask.
	         * 0 = flash fired
	         * 1 = return detected
	         * 2 = return able to be detected
	         * 3 = unknown
	         * 4 = auto used
	         * 5 = unknown
	         * 6 = red eye reduction used
	         */

	        final Integer value = _directory.getInteger(ExifDirectoryBase.TAG_FLASH);

	        if (value == null)
	            return null;

	        StringBuilder sb = new StringBuilder();

	        if ((value & 0x1) != 0)
	            sb.append("Flash fired");
	        else
	            sb.append("Flash did not fire");

	        // check if we're able to detect a return, before we mention it
	        if ((value & 0x4) != 0) {
	            if ((value & 0x2) != 0)
	                sb.append(", return detected");
	            else
	                sb.append(", return not detected");
	        }

	        // If 0x10 is set and the lowest byte is not zero - then flash is Auto
	        if ((value & 0x10) != 0 && (value & 0x0F) != 0)
	            sb.append(", auto");

	        if ((value & 0x40) != 0)
	            sb.append(", red-eye reduction");

	        return sb.toString();
	    }

	    @Nullable
	    public String getFocalLengthDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_FOCAL_LENGTH);
	        return value == null ? null : getFocalLengthDescription(value.doubleValue());
	    }

	    @Nullable
	    public String getUserCommentDescription()
	    {
	        return getEncodedTextDescription(ExifDirectoryBase.TAG_USER_COMMENT);
	    }

	    @Nullable
	    public String getTemperatureDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_TEMPERATURE);
	        if (value == null)
	            return null;
	        if (value.getDenominator() == 0xFFFFFFFFL)
	            return "Unknown";
	        DecimalFormat formatter = new DecimalFormat("0.0");
	        return formatter.format(value.doubleValue()) + " Â°C";
	    }

	    @Nullable
	    public String getHumidityDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_HUMIDITY);
	        if (value == null)
	            return null;
	        if (value.getDenominator() == 0xFFFFFFFFL)
	            return "Unknown";
	        DecimalFormat formatter = new DecimalFormat("0.0");
	        return formatter.format(value.doubleValue()) + " %";
	    }

	    @Nullable
	    public String getPressureDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_PRESSURE);
	        if (value == null)
	            return null;
	        if (value.getDenominator() == 0xFFFFFFFFL)
	            return "Unknown";
	        DecimalFormat formatter = new DecimalFormat("0.0");
	        return formatter.format(value.doubleValue()) + " hPa";
	    }

	    @Nullable
	    public String getWaterDepthDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_WATER_DEPTH);
	        if (value == null)
	            return null;
	        if (value.getDenominator() == 0xFFFFFFFFL)
	            return "Unknown";
	        DecimalFormat formatter = new DecimalFormat("0.0##");
	        return formatter.format(value.doubleValue()) + " metres";
	    }

	    @Nullable
	    public String getAccelerationDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_ACCELERATION);
	        if (value == null)
	            return null;
	        if (value.getDenominator() == 0xFFFFFFFFL)
	            return "Unknown";
	        DecimalFormat formatter = new DecimalFormat("0.0##");
	        return formatter.format(value.doubleValue()) + " mGal";
	    }

	    @Nullable
	    public String getCameraElevationAngleDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_CAMERA_ELEVATION_ANGLE);
	        if (value == null)
	            return null;
	        if (value.getDenominator() == 0xFFFFFFFFL)
	            return "Unknown";
	        DecimalFormat formatter = new DecimalFormat("0.##");
	        return formatter.format(value.doubleValue()) + " degrees";
	    }

	    /** The Windows specific tags uses plain Unicode. */
	    @Nullable
	    private String getUnicodeDescription(int tag)
	    {
	        byte[] bytes = _directory.getByteArray(tag);
	        if (bytes == null)
	            return null;
	        try {
	            // Decode the unicode string and trim the unicode zero "\0" from the end.
	            return new String(bytes, "UTF-16LE").trim();
	        } catch (UnsupportedEncodingException ex) {
	            return null;
	        }
	    }

	    @Nullable
	    public String getWindowsTitleDescription()
	    {
	        return getUnicodeDescription(ExifDirectoryBase.TAG_WIN_TITLE);
	    }

	    @Nullable
	    public String getWindowsCommentDescription()
	    {
	        return getUnicodeDescription(ExifDirectoryBase.TAG_WIN_COMMENT);
	    }

	    @Nullable
	    public String getWindowsAuthorDescription()
	    {
	        return getUnicodeDescription(ExifDirectoryBase.TAG_WIN_AUTHOR);
	    }

	    @Nullable
	    public String getWindowsKeywordsDescription()
	    {
	        return getUnicodeDescription(ExifDirectoryBase.TAG_WIN_KEYWORDS);
	    }

	    @Nullable
	    public String getWindowsSubjectDescription()
	    {
	        return getUnicodeDescription(ExifDirectoryBase.TAG_WIN_SUBJECT);
	    }

	    @Nullable
	    public String getFlashPixVersionDescription()
	    {
	        return getVersionBytesDescription(ExifDirectoryBase.TAG_FLASHPIX_VERSION, 2);
	    }

	    @Nullable
	    public String getColorSpaceDescription()
	    {
	        final Integer value = _directory.getInteger(ExifDirectoryBase.TAG_COLOR_SPACE);
	        if (value == null)
	            return null;
	        if (value == 1)
	            return "sRGB";
	        if (value == 65535)
	            return "Undefined";
	        return "Unknown (" + value + ")";
	    }

	    @Nullable
	    public String getExifImageWidthDescription()
	    {
	        final Integer value = _directory.getInteger(ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH);
	        return value == null ? null : value + " pixels";
	    }

	    @Nullable
	    public String getExifImageHeightDescription()
	    {
	        final Integer value = _directory.getInteger(ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT);
	        return value == null ? null : value + " pixels";
	    }

	    @Nullable
	    public String getFocalPlaneXResolutionDescription()
	    {
	        Rational rational = _directory.getRational(ExifDirectoryBase.TAG_FOCAL_PLANE_X_RESOLUTION);
	        if (rational == null)
	            return null;
	        final String unit = getFocalPlaneResolutionUnitDescription();
	        return rational.getReciprocal().toSimpleString(_allowDecimalRepresentationOfRationals)
	            + (unit == null ? "" : " " + unit.toLowerCase());
	    }

	    @Nullable
	    public String getFocalPlaneYResolutionDescription()
	    {
	        Rational rational = _directory.getRational(ExifDirectoryBase.TAG_FOCAL_PLANE_Y_RESOLUTION);
	        if (rational == null)
	            return null;
	        final String unit = getFocalPlaneResolutionUnitDescription();
	        return rational.getReciprocal().toSimpleString(_allowDecimalRepresentationOfRationals)
	            + (unit == null ? "" : " " + unit.toLowerCase());
	    }

	    @Nullable
	    public String getFocalPlaneResolutionUnitDescription()
	    {
	        // Unit of FocalPlaneXResolution/FocalPlaneYResolution.
	        // '1' means no-unit, '2' inch, '3' centimeter.
	        return getIndexedDescription(ExifDirectoryBase.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
	            1,
	            "(No unit)",
	            "Inches",
	            "cm"
	        );
	    }

	    @Nullable
	    public String getSensingMethodDescription()
	    {
	        // '1' Not defined, '2' One-chip color area sensor, '3' Two-chip color area sensor
	        // '4' Three-chip color area sensor, '5' Color sequential area sensor
	        // '7' Trilinear sensor '8' Color sequential linear sensor,  'Other' reserved
	        return getIndexedDescription(ExifDirectoryBase.TAG_SENSING_METHOD,
	            1,
	            "(Not defined)",
	            "One-chip color area sensor",
	            "Two-chip color area sensor",
	            "Three-chip color area sensor",
	            "Color sequential area sensor",
	            null,
	            "Trilinear sensor",
	            "Color sequential linear sensor"
	        );
	    }

	    @Nullable
	    public String getFileSourceDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_FILE_SOURCE,
	            1,
	            "Film Scanner",
	            "Reflection Print Scanner",
	            "Digital Still Camera (DSC)"
	        );
	    }

	    @Nullable
	    public String getSceneTypeDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_SCENE_TYPE,
	            1,
	            "Directly photographed image"
	        );
	    }

	    /**
	     * String description of CFA Pattern
	     *
	     * Converted from Exiftool version 10.33 created by Phil Harvey
	     * http://www.sno.phy.queensu.ca/~phil/exiftool/
	     * lib\Image\ExifTool\Exif.pm
	     *
	     * Indicates the color filter array (CFA) geometric pattern of the image sensor when a one-chip color area sensor is used.
	     * It does not apply to all sensing methods.
	     */
	    @Nullable
	    public String getCfaPatternDescription()
	    {
	        return formatCFAPattern(decodeCfaPattern(ExifDirectoryBase.TAG_CFA_PATTERN));
	    }

	    /**
	     * Decode raw CFAPattern value
	     *
	     * Converted from Exiftool version 10.33 created by Phil Harvey
	     * http://www.sno.phy.queensu.ca/~phil/exiftool/
	     * lib\Image\ExifTool\Exif.pm
	     *
	     * The value consists of:
	     * - Two short, being the grid width and height of the repeated pattern.
	     * - Next, for every pixel in that pattern, an identification code.
	     */
	    @Nullable
	    private int[] decodeCfaPattern(int tagType)
	    {
	        int[] ret;

	        byte[] values = _directory.getByteArray(tagType);
	        if (values == null)
	            return null;

	        if (values.length < 4)
	        {
	            ret = new int[values.length];
	            for (int i = 0; i < values.length; i++)
	                ret[i] = values[i];
	            return ret;
	        }

	        ret = new int[values.length - 2];

	        try {
	            ByteArrayReader reader = new ByteArrayReader(values);

	            // first two values should be read as 16-bits (2 bytes)
	            short item0 = reader.getInt16(0);
	            short item1 = reader.getInt16(2);

	            boolean copyArray = false;
	            int end = 2 + item0 * item1;
	            if (end > values.length) // sanity check in case of byte order problems; calculated 'end' should be <= length of the values
	            {
	                // try swapping byte order (I have seen this order different than in EXIF)
	                reader.setMotorolaByteOrder(!reader.isMotorolaByteOrder());
	                item0 = reader.getInt16(0);
	                item1 = reader.getInt16(2);

	                if (values.length >= (2 + item0 * item1))
	                    copyArray = true;
	            }
	            else
	                copyArray = true;

	            if(copyArray)
	            {
	                ret[0] = item0;
	                ret[1] = item1;

	                for (int i = 4; i < values.length; i++)
	                    ret[i - 2] = reader.getInt8(i);
	            }
	        } catch (IOException ex) {
	            _directory.addError("IO exception processing data: " + ex.getMessage());
	        }

	        return ret;
	    }

	    @Nullable
	    public String getCustomRenderedDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_CUSTOM_RENDERED,
	            "Normal process",
	            "Custom process"
	        );
	    }

	    @Nullable
	    public String getExposureModeDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_EXPOSURE_MODE,
	            "Auto exposure",
	            "Manual exposure",
	            "Auto bracket"
	        );
	    }

	    @Nullable
	    public String getWhiteBalanceModeDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_WHITE_BALANCE_MODE,
	            "Auto white balance",
	            "Manual white balance"
	        );
	    }

	    @Nullable
	    public String getDigitalZoomRatioDescription()
	    {
	        Rational value = _directory.getRational(ExifDirectoryBase.TAG_DIGITAL_ZOOM_RATIO);
	        return value == null
	            ? null
	            : value.getNumerator() == 0
	                ? "Digital zoom not used"
	                : new DecimalFormat("0.#").format(value.doubleValue());
	    }

	    @Nullable
	    public String get35mmFilmEquivFocalLengthDescription()
	    {
	        Integer value = _directory.getInteger(ExifDirectoryBase.TAG_35MM_FILM_EQUIV_FOCAL_LENGTH);
	        return value == null
	            ? null
	            : value == 0
	                ? "Unknown"
	                : getFocalLengthDescription(value);
	    }

	    @Nullable
	    public String getSceneCaptureTypeDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_SCENE_CAPTURE_TYPE,
	            "Standard",
	            "Landscape",
	            "Portrait",
	            "Night scene"
	        );
	    }

	    @Nullable
	    public String getGainControlDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_GAIN_CONTROL,
	            "None",
	            "Low gain up",
	            "Low gain down",
	            "High gain up",
	            "High gain down"
	        );
	    }

	    @Nullable
	    public String getContrastDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_CONTRAST,
	            "None",
	            "Soft",
	            "Hard"
	        );
	    }

	    @Nullable
	    public String getSaturationDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_SATURATION,
	            "None",
	            "Low saturation",
	            "High saturation"
	        );
	    }

	    @Nullable
	    public String getSharpnessDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_SHARPNESS,
	            "None",
	            "Low",
	            "Hard"
	        );
	    }

	    @Nullable
	    public String getSubjectDistanceRangeDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_SUBJECT_DISTANCE_RANGE,
	            "Unknown",
	            "Macro",
	            "Close view",
	            "Distant view"
	        );
	    }

	    @Nullable
	    public String getLensSpecificationDescription()
	    {
	        return getLensSpecificationDescription(ExifDirectoryBase.TAG_LENS_SPECIFICATION);
	    }

	    @Nullable
	    public String getExtraSamplesDescription()
	    {
	        return getIndexedDescription(ExifDirectoryBase.TAG_EXTRA_SAMPLES,
	            "Unspecified",
	            "Associated alpha",
	            "Unassociated alpha"
	        );
	    }

	    @Nullable
	    public String getSampleFormatDescription()
	    {
	        int[] values = _directory.getIntArray(ExifDirectoryBase.TAG_SAMPLE_FORMAT);

	        if (values == null)
	            return null;

	        StringBuilder sb = new StringBuilder();

	        for (int value : values) {
	            if (sb.length() != 0)
	                sb.append(", ");
	            switch (value){
	                case 1: sb.append("Unsigned"); break;
	                case 2: sb.append("Signed"); break;
	                case 3: sb.append("Float"); break;
	                case 4: sb.append("Undefined"); break;
	                case 5: sb.append("Complex int"); break;
	                case 6: sb.append("Complex float"); break;
	                default: sb.append("Unknown (").append(value).append(")"); break;
	            }
	        }

	        return sb.toString();
	    }
	}

	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link ExifIFD0Directory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifIFD0Descriptor extends ExifDescriptorBase<ExifIFD0Directory>
	{
	    public ExifIFD0Descriptor(@NotNull ExifIFD0Directory directory)
	    {
	        super(directory);
	    }
	}


	
	/**
	 * Describes Exif tags from the IFD0 directory.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifIFD0Directory extends ExifDirectoryBase
	{
	    /** This tag is a pointer to the Exif SubIFD. */
	    public static final int TAG_EXIF_SUB_IFD_OFFSET = 0x8769;

	    /** This tag is a pointer to the Exif GPS IFD. */
	    public static final int TAG_GPS_INFO_OFFSET = 0x8825;

	    public ExifIFD0Directory()
	    {
	        this.setDescriptor(new ExifIFD0Descriptor(this));
	    }

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    
	    {
	        addExifTagNames(_tagNameMap);
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Exif IFD0";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}


	
	/**
	 * Implementation of {@link com.drew.imaging.tiff.TiffHandler} used for handling TIFF tags according to the Exif
	 * standard.
	 * <p>
	 * Includes support for camera manufacturer makernotes.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public class ExifTiffHandler extends DirectoryTiffHandler
	{
	    public ExifTiffHandler(@NotNull Metadata metadata, @Nullable Directory parentDirectory)
	    {
	        super(metadata, parentDirectory);
	    }

	    public void setTiffMarker(int marker) throws TiffProcessingException
	    {
	        final int standardTiffMarker = 0x002A;
	        final int olympusRawTiffMarker = 0x4F52; // for ORF files
	        final int olympusRawTiffMarker2 = 0x5352; // for ORF files
	        final int panasonicRawTiffMarker = 0x0055; // for RW2 files

	        switch (marker) {
	            case standardTiffMarker:
	            case olympusRawTiffMarker:      // TODO implement an IFD0, if there is one
	            case olympusRawTiffMarker2:     // TODO implement an IFD0, if there is one
	                pushDirectory(ExifIFD0Directory.class);
	                break;
	            case panasonicRawTiffMarker:
	                pushDirectory(PanasonicRawIFD0Directory.class);
	                break;
	            default:
	                throw new TiffProcessingException(String.format("Unexpected TIFF marker: 0x%X", marker));
	        }
	    }

	    public boolean tryEnterSubIfd(int tagId)
	    {
	        if (tagId == ExifDirectoryBase.TAG_SUB_IFD_OFFSET) {
	            pushDirectory(ExifSubIFDDirectory.class);
	            return true;
	        }

	        if (_currentDirectory instanceof ExifIFD0Directory || _currentDirectory instanceof PanasonicRawIFD0Directory) {
	            if (tagId == ExifIFD0Directory.TAG_EXIF_SUB_IFD_OFFSET) {
	                pushDirectory(ExifSubIFDDirectory.class);
	                return true;
	            }

	            if (tagId == ExifIFD0Directory.TAG_GPS_INFO_OFFSET) {
	                pushDirectory(GpsDirectory.class);
	                return true;
	            }
	        } else if (_currentDirectory instanceof ExifSubIFDDirectory) {
	            if (tagId == ExifSubIFDDirectory.TAG_INTEROP_OFFSET) {
	                pushDirectory(ExifInteropDirectory.class);
	                return true;
	            }
	            
	        } 
	        else if (_currentDirectory instanceof OlympusMakernoteDirectory) {
	            // Note: these also appear in customProcessTag because some are IFD pointers while others begin immediately
	            // for the same directories
	            switch(tagId) {
	                case OlympusMakernoteDirectory.TAG_EQUIPMENT:
	                    pushDirectory(OlympusEquipmentMakernoteDirectory.class);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_CAMERA_SETTINGS:
	                    pushDirectory(OlympusCameraSettingsMakernoteDirectory.class);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_RAW_DEVELOPMENT:
	                    pushDirectory(OlympusRawDevelopmentMakernoteDirectory.class);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_RAW_DEVELOPMENT_2:
	                    pushDirectory(OlympusRawDevelopment2MakernoteDirectory.class);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_IMAGE_PROCESSING:
	                    pushDirectory(OlympusImageProcessingMakernoteDirectory.class);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_FOCUS_INFO:
	                    pushDirectory(OlympusFocusInfoMakernoteDirectory.class);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_RAW_INFO:
	                    pushDirectory(OlympusRawInfoMakernoteDirectory.class);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_MAIN_INFO:
	                    pushDirectory(OlympusMakernoteDirectory.class);
	                    return true;
	            }
	        }

	        return false;
	    }

	    public boolean hasFollowerIfd()
	    {
	        // In Exif, the only known 'follower' IFD is the thumbnail one, however this may not be the case.
	        // UPDATE: In multipage TIFFs, the 'follower' IFD points to the next image in the set
	        if (_currentDirectory instanceof ExifIFD0Directory || _currentDirectory instanceof ExifImageDirectory) {
	            // If the PageNumber tag is defined, assume this is a multipage TIFF or similar
	            // TODO: Find better ways to know which follower Directory should be used
	            if (_currentDirectory.containsTag(ExifDirectoryBase.TAG_PAGE_NUMBER))
	                pushDirectory(ExifImageDirectory.class);
	            else
	                pushDirectory(ExifThumbnailDirectory.class);
	            return true;
	        }

	        // The Canon EOS 7D (CR2) has three chained/following thumbnail IFDs
	        if (_currentDirectory instanceof ExifThumbnailDirectory)
	            return true;

	        // This should not happen, as Exif doesn't use follower IFDs apart from that above.
	        // NOTE have seen the CanonMakernoteDirectory IFD have a follower pointer, but it points to invalid data.
	        return false;
	    }

	    @Nullable
	    public Long tryCustomProcessFormat(final int tagId, final int formatCode, final long componentCount)
	    {
	        if (formatCode == 13)
	            return componentCount * 4;

	        // an unknown (0) formatCode needs to be potentially handled later as a highly custom directory tag
	        if (formatCode == 0)
	            return 0L;

	        return null;
	    }

	    public boolean customProcessTag(final int tagOffset,
	                                    final @NotNull Set<Integer> processedIfdOffsets,
	                                    final int tiffHeaderOffset,
	                                    final @NotNull RandomAccessReader reader,
	                                    final int tagId,
	                                    final int byteCount) throws IOException
	    {
	        assert(_currentDirectory != null);

	        // Some 0x0000 tags have a 0 byteCount. Determine whether it's bad.
	        if (tagId == 0) {
	            if (_currentDirectory.containsTag(tagId)) {
	                // Let it go through for now. Some directories handle it, some don't
	                return false;
	            }

	            // Skip over 0x0000 tags that don't have any associated bytes. No idea what it contains in this case, if anything.
	            if (byteCount == 0)
	                return true;
	        }

	        // Custom processing for the Makernote tag
	        if (tagId == ExifSubIFDDirectory.TAG_MAKERNOTE && _currentDirectory instanceof ExifSubIFDDirectory) {
	            return processMakernote(tagOffset, processedIfdOffsets, tiffHeaderOffset, reader);
	        }

	        
	        // Custom processing for embedded IPTC data
	        /*
	        if (tagId == ExifSubIFDDirectory.TAG_IPTC_NAA && _currentDirectory instanceof ExifIFD0Directory) {
	            // NOTE Adobe sets type 4 for IPTC instead of 7
	            if (reader.getInt8(tagOffset) == 0x1c) {
	                final byte[] iptcBytes = reader.getBytes(tagOffset, byteCount);
	                new IptcReader().extract(new SequentialByteArrayReader(iptcBytes), _metadata, iptcBytes.length, _currentDirectory);
	                return true;
	            }
	            return false;
	        }

	        // Custom processing for ICC Profile data
	        if (tagId == ExifSubIFDDirectory.TAG_INTER_COLOR_PROFILE) {
	            final byte[] iccBytes = reader.getBytes(tagOffset, byteCount);
	            new IccReader().extract(new ByteArrayReader(iccBytes), _metadata, _currentDirectory);
	            return true;
	        }

	        // Custom processing for Photoshop data
	        if (tagId == ExifSubIFDDirectory.TAG_PHOTOSHOP_SETTINGS && _currentDirectory instanceof ExifIFD0Directory) {
	            final byte[] photoshopBytes = reader.getBytes(tagOffset, byteCount);
	            new PhotoshopReader().extract(new SequentialByteArrayReader(photoshopBytes), byteCount, _metadata, _currentDirectory);
	            return true;
	        }

	        // Custom processing for embedded XMP data
	        if (tagId == ExifSubIFDDirectory.TAG_APPLICATION_NOTES && (_currentDirectory instanceof ExifIFD0Directory || _currentDirectory instanceof ExifSubIFDDirectory)) {
	            new XmpReader().extract(reader.getNullTerminatedBytes(tagOffset, byteCount), _metadata, _currentDirectory);
	            return true;
	        }

	        // Custom processing for Apple RunTime tag
	        if (tagId == AppleMakernoteDirectory.TAG_RUN_TIME && _currentDirectory instanceof AppleMakernoteDirectory) {
	            byte[] bytes = reader.getBytes(tagOffset, byteCount);
	            new AppleRunTimeReader().extract(bytes, _metadata, _currentDirectory);
	            return true;
	        }

	        if (handlePrintIM(_currentDirectory, tagId))
	        {
	            PrintIMDirectory printIMDirectory = new PrintIMDirectory();
	            printIMDirectory.setParent(_currentDirectory);
	            _metadata.addDirectory(printIMDirectory);
	            processPrintIM(printIMDirectory, tagOffset, reader, byteCount);
	            return true;
	        }
	        */

	        // Note: these also appear in tryEnterSubIfd because some are IFD pointers while others begin immediately
	        // for the same directories
	        if (_currentDirectory instanceof OlympusMakernoteDirectory) {
	        	TiffReader tr = new TiffReader();
	            switch (tagId) {
	                case OlympusMakernoteDirectory.TAG_EQUIPMENT:
	                    pushDirectory(OlympusEquipmentMakernoteDirectory.class);
	                    tr.processIfd(this, reader, processedIfdOffsets, tagOffset, tiffHeaderOffset);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_CAMERA_SETTINGS:
	                    pushDirectory(OlympusCameraSettingsMakernoteDirectory.class);
	                    tr.processIfd(this, reader, processedIfdOffsets, tagOffset, tiffHeaderOffset);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_RAW_DEVELOPMENT:
	                    pushDirectory(OlympusRawDevelopmentMakernoteDirectory.class);
	                    tr.processIfd(this, reader, processedIfdOffsets, tagOffset, tiffHeaderOffset);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_RAW_DEVELOPMENT_2:
	                    pushDirectory(OlympusRawDevelopment2MakernoteDirectory.class);
	                    tr.processIfd(this, reader, processedIfdOffsets, tagOffset, tiffHeaderOffset);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_IMAGE_PROCESSING:
	                    pushDirectory(OlympusImageProcessingMakernoteDirectory.class);
	                    tr.processIfd(this, reader, processedIfdOffsets, tagOffset, tiffHeaderOffset);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_FOCUS_INFO:
	                    pushDirectory(OlympusFocusInfoMakernoteDirectory.class);
	                    tr.processIfd(this, reader, processedIfdOffsets, tagOffset, tiffHeaderOffset);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_RAW_INFO:
	                    pushDirectory(OlympusRawInfoMakernoteDirectory.class);
	                    tr.processIfd(this, reader, processedIfdOffsets, tagOffset, tiffHeaderOffset);
	                    return true;
	                case OlympusMakernoteDirectory.TAG_MAIN_INFO:
	                    pushDirectory(OlympusMakernoteDirectory.class);
	                    tr.processIfd(this, reader, processedIfdOffsets, tagOffset, tiffHeaderOffset);
	                    return true;
	            }
	        }

	        if (_currentDirectory instanceof PanasonicRawIFD0Directory) {
	            // these contain binary data with specific offsets, and can't be processed as regular ifd's.
	            // The binary data is broken into 'fake' tags and there is a pattern.
	            switch (tagId) {
	                case PanasonicRawIFD0Directory.TagWbInfo:
	                    PanasonicRawWbInfoDirectory dirWbInfo = new PanasonicRawWbInfoDirectory();
	                    dirWbInfo.setParent(_currentDirectory);
	                    _metadata.addDirectory(dirWbInfo);
	                    processBinary(dirWbInfo, tagOffset, reader, byteCount, false, 2);
	                    return true;
	                case PanasonicRawIFD0Directory.TagWbInfo2:
	                    PanasonicRawWbInfo2Directory dirWbInfo2 = new PanasonicRawWbInfo2Directory();
	                    dirWbInfo2.setParent(_currentDirectory);
	                    _metadata.addDirectory(dirWbInfo2);
	                    processBinary(dirWbInfo2, tagOffset, reader, byteCount, false, 3);
	                    return true;
	                case PanasonicRawIFD0Directory.TagDistortionInfo:
	                    PanasonicRawDistortionDirectory dirDistort = new PanasonicRawDistortionDirectory();
	                    dirDistort.setParent(_currentDirectory);
	                    _metadata.addDirectory(dirDistort);
	                    processBinary(dirDistort, tagOffset, reader, byteCount, true, 1);
	                    return true;
	            }
	        }

	        // Panasonic RAW sometimes contains an embedded version of the data as a JPG file.
	        if (tagId == PanasonicRawIFD0Directory.TagJpgFromRaw && _currentDirectory instanceof PanasonicRawIFD0Directory) {
	            byte[] jpegrawbytes = reader.getBytes(tagOffset, byteCount);

	            // Extract information from embedded image since it is metadata-rich
	            ByteArrayInputStream jpegmem = new ByteArrayInputStream(jpegrawbytes);
	            try {
	            	JpegMetadataReader jr = new JpegMetadataReader();
	                Metadata jpegDirectory = jr.readMetadata(jpegmem);
	                for (Directory directory : jpegDirectory.getDirectories()) {
	                    directory.setParent(_currentDirectory);
	                    _metadata.addDirectory(directory);
	                }
	                return true;
	            } catch (JpegProcessingException e) {
	                _currentDirectory.addError("Error processing JpgFromRaw: " + e.getMessage());
	            } catch (IOException e) {
	                _currentDirectory.addError("Error reading JpgFromRaw: " + e.getMessage());
	            }
	        }

	        if (_currentDirectory instanceof SonyType1MakernoteDirectory) {
	            if (tagId == SonyType1MakernoteDirectory.TAG_9050B) {
	                byte[] bytes = reader.getBytes(tagOffset, byteCount);
	                SonyTag9050bDirectory st = new SonyTag9050bDirectory();
	                st.read(bytes);
	                st.setParent(_currentDirectory);
	                _metadata.addDirectory(st);
	                return true;
	            }
	        }

	        return false;
	    }

	    private void processBinary(@NotNull final Directory directory, final int tagValueOffset, @NotNull final RandomAccessReader reader, final int byteCount, final Boolean isSigned, final int arrayLength) throws IOException
	    {
	        // expects signed/unsigned int16 (for now)
	        //int byteSize = isSigned ? sizeof(short) : sizeof(ushort);
	        int byteSize = 2;

	        // 'directory' is assumed to contain tags that correspond to the byte position unless it's a set of bytes
	        for (int i = 0; i < byteCount; i++) {
	            if (directory.hasTagName(i)) {
	                // only process this tag if the 'next' integral tag exists. Otherwise, it's a set of bytes
	                if (i < byteCount - 1 && directory.hasTagName(i + 1)) {
	                    if (isSigned)
	                        directory.setObject(i, reader.getInt16(tagValueOffset + (i* byteSize)));
	                    else
	                        directory.setObject(i, reader.getUInt16(tagValueOffset + (i* byteSize)));
	                } else {
	                    // the next arrayLength bytes are a multi-byte value
	                    if (isSigned) {
	                        short[] val = new short[arrayLength];
	                        for (int j = 0; j<val.length; j++)
	                            val[j] = reader.getInt16(tagValueOffset + ((i + j) * byteSize));
	                        directory.setObjectArray(i, val);
	                    } else {
	                        int[] val = new int[arrayLength];
	                        for (int j = 0; j<val.length; j++)
	                            val[j] = reader.getUInt16(tagValueOffset + ((i + j) * byteSize));
	                        directory.setObjectArray(i, val);
	                    }

	                    i += arrayLength - 1;
	                }
	            }
	        }
	    }

	    /** Read a given number of bytes from the stream
	     *
	     * This method is employed to "suppress" attempts to read beyond end of the
	     * file as may happen at the beginning of processMakernote when we read
	     * increasingly longer camera makes.
	     *
	     * Instead of failing altogether in this context we return an empty string
	     * a full-on failure.
	     */
	    @NotNull
	    private String getReaderString(final @NotNull RandomAccessReader reader, final int makernoteOffset, final int bytesRequested) throws IOException
	    
	    {
	        try {
	        	Charsets ch = new Charsets();
	            return reader.getString(makernoteOffset, bytesRequested, ch.UTF_8);
	        } catch(BufferBoundsException e) {
	            return "";
	        }
	    }

	    private boolean processMakernote(final int makernoteOffset,
	                                     final @NotNull Set<Integer> processedIfdOffsets,
	                                     final int tiffHeaderOffset,
	                                     final @NotNull RandomAccessReader reader) throws IOException
	    {
	        assert(_currentDirectory != null);

	        // Determine the camera model and makernote format.
	        Directory ifd0Directory = _metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

	        String cameraMake = ifd0Directory == null ? null : ifd0Directory.getString(ExifIFD0Directory.TAG_MAKE);

	        final String firstTwoChars    = getReaderString(reader, makernoteOffset, 2);
	        final String firstThreeChars  = getReaderString(reader, makernoteOffset, 3);
	        final String firstFourChars   = getReaderString(reader, makernoteOffset, 4);
	        final String firstFiveChars   = getReaderString(reader, makernoteOffset, 5);
	        final String firstSixChars    = getReaderString(reader, makernoteOffset, 6);
	        final String firstSevenChars  = getReaderString(reader, makernoteOffset, 7);
	        final String firstEightChars  = getReaderString(reader, makernoteOffset, 8);
	        final String firstNineChars   = getReaderString(reader, makernoteOffset, 9);
	        final String firstTenChars    = getReaderString(reader, makernoteOffset, 10);
	        final String firstTwelveChars = getReaderString(reader, makernoteOffset, 12);

	        boolean byteOrderBefore = reader.isMotorolaByteOrder();
	        TiffReader tr = new TiffReader();

	        if ("OLYMP\0".equals(firstSixChars) || "EPSON".equals(firstFiveChars) || "AGFA".equals(firstFourChars)) {
	            // Olympus Makernote
	            // Epson and Agfa use Olympus makernote standard: http://www.ozhiker.com/electronics/pjmt/jpeg_info/
	            pushDirectory(OlympusMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 8, tiffHeaderOffset);
	        } else if ("OLYMPUS\0II".equals(firstTenChars)) {
	            // Olympus Makernote (alternate)
	            // Note that data is relative to the beginning of the makernote
	            // http://exiv2.org/makernote.html
	            pushDirectory(OlympusMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 12, makernoteOffset);
	        } else if (cameraMake != null && cameraMake.toUpperCase().startsWith("MINOLTA")) {
	            // Cases seen with the model starting with MINOLTA in capitals seem to have a valid Olympus makernote
	            // area that commences immediately.
	            pushDirectory(OlympusMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset, tiffHeaderOffset);
	        
	        } 
	        else if (cameraMake != null && cameraMake.trim().toUpperCase().startsWith("NIKON")) {
	            if ("Nikon".equals(firstFiveChars)) {
	                /* There are two scenarios here:
	                 * Type 1:                  **
	                 * :0000: 4E 69 6B 6F 6E 00 01 00-05 00 02 00 02 00 06 00 Nikon...........
	                 * :0010: 00 00 EC 02 00 00 03 00-03 00 01 00 00 00 06 00 ................
	                 * Type 3:                  **
	                 * :0000: 4E 69 6B 6F 6E 00 02 00-00 00 4D 4D 00 2A 00 00 Nikon....MM.*...
	                 * :0010: 00 08 00 1E 00 01 00 07-00 00 00 04 30 32 30 30 ............0200
	                 */
	            
	                switch (reader.getUInt8(makernoteOffset + 6)) {
	                    case 1:
	                        pushDirectory(NikonType1MakernoteDirectory.class);
	                        tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 8, tiffHeaderOffset);
	                        break;
	                    case 2:
	                        pushDirectory(NikonType2MakernoteDirectory.class);
	                        tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 18, makernoteOffset + 10);
	                        break;
	                    default:
	                        _currentDirectory.addError("Unsupported Nikon makernote data ignored.");
	                        break;
	                }
	            } else {
	                // The IFD begins with the first Makernote byte (no ASCII name).  This occurs with CoolPix 775, E990 and D1 models.
	                pushDirectory(NikonType2MakernoteDirectory.class);
	                tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset, tiffHeaderOffset);
	            }
	        } else if ("SONY CAM".equals(firstEightChars) || "SONY DSC".equals(firstEightChars)) {
	            pushDirectory(SonyType1MakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 12, tiffHeaderOffset);
	        // Do this check LAST after most other Sony checks
	        } else if (cameraMake != null && cameraMake.startsWith("SONY") &&
	                !Arrays.equals(reader.getBytes(makernoteOffset, 2), new byte[]{ 0x01, 0x00 }) ) {
	            // The IFD begins with the first Makernote byte (no ASCII name). Used in SR2 and ARW images
	            pushDirectory(SonyType1MakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset, tiffHeaderOffset);
	        } else if ("SEMC MS\u0000\u0000\u0000\u0000\u0000".equals(firstTwelveChars)) {
	            // force MM for this directory
	            reader.setMotorolaByteOrder(true);
	            // skip 12 byte header + 2 for "MM" + 6
	            pushDirectory(SonyType6MakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 20, tiffHeaderOffset);
	        } 
	        /*
	        else if ("SIGMA\u0000\u0000\u0000".equals(firstEightChars) || "FOVEON\u0000\u0000".equals(firstEightChars)) {
	            pushDirectory(SigmaMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 10, tiffHeaderOffset);
	        } else if ("KDK".equals(firstThreeChars)) {
	            reader.setMotorolaByteOrder(firstSevenChars.equals("KDK INFO"));
	            KodakMakernoteDirectory directory = new KodakMakernoteDirectory();
	            _metadata.addDirectory(directory);
	            processKodakMakernote(directory, makernoteOffset, reader);
	        } else if ("Canon".equalsIgnoreCase(cameraMake)) {
	            pushDirectory(CanonMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset, tiffHeaderOffset);
	        } else if (cameraMake != null && cameraMake.toUpperCase().startsWith("CASIO")) {
	            if ("QVC\u0000\u0000\u0000".equals(firstSixChars)) {
	                pushDirectory(CasioType2MakernoteDirectory.class);
	                tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 6, tiffHeaderOffset);
	            } else {
	                pushDirectory(CasioType1MakernoteDirectory.class);
	                tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset, tiffHeaderOffset);
	            }
	        } else if ("FUJIFILM".equals(firstEightChars) || "Fujifilm".equalsIgnoreCase(cameraMake)) {
	            // Note that this also applies to certain Leica cameras, such as the Digilux-4.3
	            reader.setMotorolaByteOrder(false);
	            // the 4 bytes after "FUJIFILM" in the makernote point to the start of the makernote
	            // IFD, though the offset is relative to the start of the makernote, not the TIFF
	            // header (like everywhere else)
	            int ifdStart = makernoteOffset + reader.getInt32(makernoteOffset + 8);
	            pushDirectory(FujifilmMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, ifdStart, makernoteOffset);
	        } else if ("KYOCERA".equals(firstSevenChars)) {
	            // http://www.ozhiker.com/electronics/pjmt/jpeg_info/kyocera_mn.html
	            pushDirectory(KyoceraMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 22, tiffHeaderOffset);
	        } else if ("LEICA".equals(firstFiveChars)) {
	            reader.setMotorolaByteOrder(false);

	            // used by the X1/X2/X VARIO/T
	            // (X1 starts with "LEICA\0\x01\0", Make is "LEICA CAMERA AG")
	            // (X2 starts with "LEICA\0\x05\0", Make is "LEICA CAMERA AG")
	            // (X VARIO starts with "LEICA\0\x04\0", Make is "LEICA CAMERA AG")
	            // (T (Typ 701) starts with "LEICA\0\0x6", Make is "LEICA CAMERA AG")
	            // (X (Typ 113) starts with "LEICA\0\0x7", Make is "LEICA CAMERA AG")

	            if ("LEICA\0\u0001\0".equals(firstEightChars) ||
	                "LEICA\0\u0004\0".equals(firstEightChars) ||
	                "LEICA\0\u0005\0".equals(firstEightChars) ||
	                "LEICA\0\u0006\0".equals(firstEightChars) ||
	                "LEICA\0\u0007\0".equals(firstEightChars))
	            {
	                pushDirectory(LeicaType5MakernoteDirectory.class);
	                tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 8, makernoteOffset);
	            } else if ("Leica Camera AG".equals(cameraMake)) {
	                pushDirectory(LeicaMakernoteDirectory.class);
	                tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 8, tiffHeaderOffset);
	            } else if ("LEICA".equals(cameraMake)) {
	                // Some Leica cameras use Panasonic makernote tags
	                pushDirectory(PanasonicMakernoteDirectory.class);
	                tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 8, tiffHeaderOffset);
	            } else {
	                return false;
	            }
	        } else if ("Panasonic\u0000\u0000\u0000".equals(firstTwelveChars)) {
	            // NON-Standard TIFF IFD Data using Panasonic Tags. There is no Next-IFD pointer after the IFD
	            // Offsets are relative to the start of the TIFF header at the beginning of the EXIF segment
	            // more information here: http://www.ozhiker.com/electronics/pjmt/jpeg_info/panasonic_mn.html
	            pushDirectory(PanasonicMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 12, tiffHeaderOffset);
	        } else if ("AOC\u0000".equals(firstFourChars)) {
	            // NON-Standard TIFF IFD Data using Casio Type 2 Tags
	            // IFD has no Next-IFD pointer at end of IFD, and
	            // Offsets are relative to the start of the current IFD tag, not the TIFF header
	            // Observed for:
	            // - Pentax ist D
	            pushDirectory(CasioType2MakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 6, makernoteOffset);
	        } else if (cameraMake != null && (cameraMake.toUpperCase().startsWith("PENTAX") || cameraMake.toUpperCase().startsWith("ASAHI"))) {
	            // NON-Standard TIFF IFD Data using Pentax Tags
	            // IFD has no Next-IFD pointer at end of IFD, and
	            // Offsets are relative to the start of the current IFD tag, not the TIFF header
	            // Observed for:
	            // - PENTAX Optio 330
	            // - PENTAX Optio 430
	            pushDirectory(PentaxMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset, makernoteOffset);
//	        } else if ("KC".equals(firstTwoChars) || "MINOL".equals(firstFiveChars) || "MLY".equals(firstThreeChars) || "+M+M+M+M".equals(firstEightChars)) {
//	            // This Konica data is not understood.  Header identified in accordance with information at this site:
//	            // http://www.ozhiker.com/electronics/pjmt/jpeg_info/minolta_mn.html
//	            // TODO add support for minolta/konica cameras
//	            exifDirectory.addError("Unsupported Konica/Minolta data ignored.");
	        } else if ("SANYO\0\1\0".equals(firstEightChars)) {
	            pushDirectory(SanyoMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 8, makernoteOffset);
	        } else if (cameraMake != null && cameraMake.toLowerCase().startsWith("ricoh")) {
	            if (firstTwoChars.equals("Rv") || firstThreeChars.equals("Rev")) {
	                // This is a textual format, where the makernote bytes look like:
	                //   Rv0103;Rg1C;Bg18;Ll0;Ld0;Aj0000;Bn0473800;Fp2E00:ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½
	                //   Rv0103;Rg1C;Bg18;Ll0;Ld0;Aj0000;Bn0473800;Fp2D05:ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½ï¿½
	                //   Rv0207;Sf6C84;Rg76;Bg60;Gg42;Ll0;Ld0;Aj0004;Bn0B02900;Fp10B8;Md6700;Ln116900086D27;Sv263:0000000000000000000000ï¿½ï¿½
	                // This format is currently unsupported
	                return false;
	            } else if (firstFiveChars.equalsIgnoreCase("Ricoh")) {
	                // Always in Motorola byte order
	                reader.setMotorolaByteOrder(true);
	                pushDirectory(RicohMakernoteDirectory.class);
	                tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 8, makernoteOffset);
	            }
	        } else if (firstTenChars.equals("Apple iOS\0")) {
	            // Always in Motorola byte order
	            boolean orderBefore = reader.isMotorolaByteOrder();
	            reader.setMotorolaByteOrder(true);
	            pushDirectory(AppleMakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset + 14, makernoteOffset);
	            reader.setMotorolaByteOrder(orderBefore);
	        } else if (reader.getUInt16(makernoteOffset) == ReconyxHyperFireMakernoteDirectory.MAKERNOTE_VERSION) {
	            ReconyxHyperFireMakernoteDirectory directory = new ReconyxHyperFireMakernoteDirectory();
	            _metadata.addDirectory(directory);
	            processReconyxHyperFireMakernote(directory, makernoteOffset, reader);
	        } else if (firstNineChars.equalsIgnoreCase("RECONYXUF")) {
	            ReconyxUltraFireMakernoteDirectory directory = new ReconyxUltraFireMakernoteDirectory();
	            _metadata.addDirectory(directory);
	            processReconyxUltraFireMakernote(directory, makernoteOffset, reader);
	        } else if (firstNineChars.equalsIgnoreCase("RECONYXH2")) {
	            ReconyxHyperFire2MakernoteDirectory directory = new ReconyxHyperFire2MakernoteDirectory();
	            _metadata.addDirectory(directory);
	            processReconyxHyperFire2Makernote(directory, makernoteOffset, reader);
	        } else if ("SAMSUNG".equalsIgnoreCase(cameraMake)) {
	            // Only handles Type2 notes correctly. Others aren't implemented, and it's complex to determine which ones to use
	            pushDirectory(SamsungType2MakernoteDirectory.class);
	            tr.processIfd(this, reader, processedIfdOffsets, makernoteOffset, tiffHeaderOffset);
	        } else {
	            // The makernote is not comprehended by this library.
	            // If you are reading this and believe a particular camera's image should be processed, get in touch.
	            return false;
	        }
	        */

	        reader.setMotorolaByteOrder(byteOrderBefore);
	        return true;
	    }

	    /*private boolean handlePrintIM(@NotNull final Directory directory, final int tagId)
	    {
	        if (tagId == ExifDirectoryBase.TAG_PRINT_IMAGE_MATCHING_INFO)
	            return true;

	        if (tagId == 0x0E00) {
	            // Tempting to say every tagid of 0x0E00 is a PIM tag, but can't be 100% sure
	            if (directory instanceof CasioType2MakernoteDirectory ||
	                directory instanceof KyoceraMakernoteDirectory ||
	                directory instanceof NikonType2MakernoteDirectory ||
	                directory instanceof OlympusMakernoteDirectory ||
	                directory instanceof PanasonicMakernoteDirectory ||
	                directory instanceof PentaxMakernoteDirectory ||
	                directory instanceof RicohMakernoteDirectory ||
	                directory instanceof SanyoMakernoteDirectory ||
	                directory instanceof SonyType1MakernoteDirectory)
	                return true;
	        }

	        return false;
	    }
	    */

	    /**
	     * Process PrintIM IFD
	     *
	     * Converted from Exiftool version 10.33 created by Phil Harvey
	     * http://www.sno.phy.queensu.ca/~phil/exiftool/
	     * lib\Image\ExifTool\PrintIM.pm
	     */
	    /*
	    private void processPrintIM(@NotNull final PrintIMDirectory directory, final int tagValueOffset, @NotNull final RandomAccessReader reader, final int byteCount) throws IOException
	    {
	        Boolean resetByteOrder = null;

	        if (byteCount == 0) {
	            directory.addError("Empty PrintIM data");
	            return;
	        }

	        if (byteCount <= 15) {
	            directory.addError("Bad PrintIM data");
	            return;
	        }

	        String header = reader.getString(tagValueOffset, 12, Charsets.UTF_8);

	        if (!header.startsWith("PrintIM")) {
	            directory.addError("Invalid PrintIM header");
	            return;
	        }

	        // check size of PrintIM block
	        int num = reader.getUInt16(tagValueOffset + 14);

	        if (byteCount < 16 + num * 6) {
	            // size is too big, maybe byte ordering is wrong
	            resetByteOrder = reader.isMotorolaByteOrder();
	            reader.setMotorolaByteOrder(!reader.isMotorolaByteOrder());
	            num = reader.getUInt16(tagValueOffset + 14);
	            if (byteCount < 16 + num * 6) {
	                directory.addError("Bad PrintIM size");
	                return;
	            }
	        }

	        if (header.length() >= 12) {
	            directory.setObject(PrintIMDirectory.TagPrintImVersion, header.substring(8, 12));
	        }

	        for (int n = 0; n < num; n++) {
	            int pos = tagValueOffset + 16 + n * 6;
	            int tag = reader.getUInt16(pos);
	            long val = reader.getUInt32(pos + 2);

	            directory.setObject(tag, val);
	        }

	        if (resetByteOrder != null)
	            reader.setMotorolaByteOrder(resetByteOrder);
	    }

	    private void processKodakMakernote(@NotNull final KodakMakernoteDirectory directory, final int tagValueOffset, @NotNull final RandomAccessReader reader)
	    {
	        // Kodak's makernote is not in IFD format. It has values at fixed offsets.
	        int dataOffset = tagValueOffset + 8;
	        try {
	            directory.setStringValue(KodakMakernoteDirectory.TAG_KODAK_MODEL, reader.getStringValue(dataOffset, 8, Charsets.UTF_8));
	            directory.setInt(KodakMakernoteDirectory.TAG_QUALITY, reader.getUInt8(dataOffset + 9));
	            directory.setInt(KodakMakernoteDirectory.TAG_BURST_MODE, reader.getUInt8(dataOffset + 10));
	            directory.setInt(KodakMakernoteDirectory.TAG_IMAGE_WIDTH, reader.getUInt16(dataOffset + 12));
	            directory.setInt(KodakMakernoteDirectory.TAG_IMAGE_HEIGHT, reader.getUInt16(dataOffset + 14));
	            directory.setInt(KodakMakernoteDirectory.TAG_YEAR_CREATED, reader.getUInt16(dataOffset + 16));
	            directory.setByteArray(KodakMakernoteDirectory.TAG_MONTH_DAY_CREATED, reader.getBytes(dataOffset + 18, 2));
	            directory.setByteArray(KodakMakernoteDirectory.TAG_TIME_CREATED, reader.getBytes(dataOffset + 20, 4));
	            directory.setInt(KodakMakernoteDirectory.TAG_BURST_MODE_2, reader.getUInt16(dataOffset + 24));
	            directory.setInt(KodakMakernoteDirectory.TAG_SHUTTER_MODE, reader.getUInt8(dataOffset + 27));
	            directory.setInt(KodakMakernoteDirectory.TAG_METERING_MODE, reader.getUInt8(dataOffset + 28));
	            directory.setInt(KodakMakernoteDirectory.TAG_SEQUENCE_NUMBER, reader.getUInt8(dataOffset + 29));
	            directory.setInt(KodakMakernoteDirectory.TAG_F_NUMBER, reader.getUInt16(dataOffset + 30));
	            directory.setLong(KodakMakernoteDirectory.TAG_EXPOSURE_TIME, reader.getUInt32(dataOffset + 32));
	            directory.setInt(KodakMakernoteDirectory.TAG_EXPOSURE_COMPENSATION, reader.getInt16(dataOffset + 36));
	            directory.setInt(KodakMakernoteDirectory.TAG_FOCUS_MODE, reader.getUInt8(dataOffset + 56));
	            directory.setInt(KodakMakernoteDirectory.TAG_WHITE_BALANCE, reader.getUInt8(dataOffset + 64));
	            directory.setInt(KodakMakernoteDirectory.TAG_FLASH_MODE, reader.getUInt8(dataOffset + 92));
	            directory.setInt(KodakMakernoteDirectory.TAG_FLASH_FIRED, reader.getUInt8(dataOffset + 93));
	            directory.setInt(KodakMakernoteDirectory.TAG_ISO_SETTING, reader.getUInt16(dataOffset + 94));
	            directory.setInt(KodakMakernoteDirectory.TAG_ISO, reader.getUInt16(dataOffset + 96));
	            directory.setInt(KodakMakernoteDirectory.TAG_TOTAL_ZOOM, reader.getUInt16(dataOffset + 98));
	            directory.setInt(KodakMakernoteDirectory.TAG_DATE_TIME_STAMP, reader.getUInt16(dataOffset + 100));
	            directory.setInt(KodakMakernoteDirectory.TAG_COLOR_MODE, reader.getUInt16(dataOffset + 102));
	            directory.setInt(KodakMakernoteDirectory.TAG_DIGITAL_ZOOM, reader.getUInt16(dataOffset + 104));
	            directory.setInt(KodakMakernoteDirectory.TAG_SHARPNESS, reader.getInt8(dataOffset + 107));
	        } catch (IOException ex) {
	            directory.addError("Error processing Kodak makernote data: " + ex.getMessage());
	        }
	    }

	    private void processReconyxHyperFireMakernote(@NotNull final ReconyxHyperFireMakernoteDirectory directory, final int makernoteOffset, @NotNull final RandomAccessReader reader) throws IOException
	    {
	        directory.setObject(ReconyxHyperFireMakernoteDirectory.TAG_MAKERNOTE_VERSION, reader.getUInt16(makernoteOffset));

	        int major = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_FIRMWARE_VERSION);
	        int minor = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_FIRMWARE_VERSION + 2);
	        int revision = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_FIRMWARE_VERSION + 4);
	        String buildYear = String.format("%04X", reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_FIRMWARE_VERSION + 6));
	        String buildDate = String.format("%04X", reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_FIRMWARE_VERSION + 8));
	        String buildYearAndDate = buildYear + buildDate;
	        Integer build;
	        try {
	            build = Integer.parseInt(buildYearAndDate);
	        } catch (NumberFormatException e) {
	            build = null;
	        }

	        if (build != null) {
	            directory.setString(ReconyxHyperFireMakernoteDirectory.TAG_FIRMWARE_VERSION, String.format("%d.%d.%d.%s", major, minor, revision, build));
	        } else {
	            directory.setString(ReconyxHyperFireMakernoteDirectory.TAG_FIRMWARE_VERSION, String.format("%d.%d.%d", major, minor, revision));
	            directory.addError("Error processing Reconyx HyperFire makernote data: build '" + buildYearAndDate + "' is not in the expected format and will be omitted from Firmware Version.");
	        }

	        directory.setString(ReconyxHyperFireMakernoteDirectory.TAG_TRIGGER_MODE, String.valueOf((char)reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_TRIGGER_MODE)));
	        directory.setIntArray(ReconyxHyperFireMakernoteDirectory.TAG_SEQUENCE,
	                      new int[]
	                      {
	                          reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_SEQUENCE),
	                          reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_SEQUENCE + 2)
	                      });

	        int eventNumberHigh = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_EVENT_NUMBER);
	        int eventNumberLow = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_EVENT_NUMBER + 2);
	        directory.setInt(ReconyxHyperFireMakernoteDirectory.TAG_EVENT_NUMBER, (eventNumberHigh << 16) + eventNumberLow);

	        int seconds = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL);
	        int minutes = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 2);
	        int hour = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 4);
	        int month = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 6);
	        int day = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 8);
	        int year = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 10);

	        if ((seconds >= 0 && seconds < 60) &&
	            (minutes >= 0 && minutes < 60) &&
	            (hour >= 0 && hour < 24) &&
	            (month >= 1 && month < 13) &&
	            (day >= 1 && day < 32) &&
	            (year >= 1 && year <= 9999)) {
	            directory.setString(ReconyxHyperFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL,
	                    String.format("%4d:%2d:%2d %2d:%2d:%2d", year, month, day, hour, minutes, seconds));
	        } else {
	            directory.addError("Error processing Reconyx HyperFire makernote data: Date/Time Original " + year + "-" + month + "-" + day + " " + hour + ":" + minutes + ":" + seconds + " is not a valid date/time.");
	        }

	        directory.setInt(ReconyxHyperFireMakernoteDirectory.TAG_MOON_PHASE, reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_MOON_PHASE));
	        directory.setInt(ReconyxHyperFireMakernoteDirectory.TAG_AMBIENT_TEMPERATURE_FAHRENHEIT, reader.getInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_AMBIENT_TEMPERATURE_FAHRENHEIT));
	        directory.setInt(ReconyxHyperFireMakernoteDirectory.TAG_AMBIENT_TEMPERATURE, reader.getInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_AMBIENT_TEMPERATURE));
	        //directory.setByteArray(ReconyxHyperFireMakernoteDirectory.TAG_SERIAL_NUMBER, reader.getBytes(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_SERIAL_NUMBER, 28));
	        directory.setStringValue(ReconyxHyperFireMakernoteDirectory.TAG_SERIAL_NUMBER, new StringValue(reader.getBytes(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_SERIAL_NUMBER, 28), Charsets.UTF_16LE));
	        // two unread bytes: the serial number's terminating null

	        directory.setInt(ReconyxHyperFireMakernoteDirectory.TAG_CONTRAST, reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_CONTRAST));
	        directory.setInt(ReconyxHyperFireMakernoteDirectory.TAG_BRIGHTNESS, reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_BRIGHTNESS));
	        directory.setInt(ReconyxHyperFireMakernoteDirectory.TAG_SHARPNESS, reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_SHARPNESS));
	        directory.setInt(ReconyxHyperFireMakernoteDirectory.TAG_SATURATION, reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_SATURATION));
	        directory.setInt(ReconyxHyperFireMakernoteDirectory.TAG_INFRARED_ILLUMINATOR, reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_INFRARED_ILLUMINATOR));
	        directory.setInt(ReconyxHyperFireMakernoteDirectory.TAG_MOTION_SENSITIVITY, reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_MOTION_SENSITIVITY));
	        directory.setDouble(ReconyxHyperFireMakernoteDirectory.TAG_BATTERY_VOLTAGE, reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_BATTERY_VOLTAGE) / 1000.0);
	        directory.setString(ReconyxHyperFireMakernoteDirectory.TAG_USER_LABEL, reader.getNullTerminatedString(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_USER_LABEL, 44, Charsets.UTF_8));
	    }

	    private void processReconyxHyperFire2Makernote(@NotNull final ReconyxHyperFire2MakernoteDirectory directory, final int makernoteOffset, @NotNull final RandomAccessReader reader) throws IOException
	    {

	        int major = reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_FIRMWARE_VERSION);
	        int minor = reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_FIRMWARE_VERSION + 2);
	        int revision = reader.getUInt16(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_FIRMWARE_VERSION + 4);
	        String buildYear = String.format("%04X", reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_FIRMWARE_VERSION + 6));
	        String buildDate = String.format("%04X", reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_FIRMWARE_VERSION + 8));
	        String buildYearAndDate = buildYear + buildDate;
	        Integer build;
	        try {
	            build = Integer.parseInt(buildYearAndDate);
	        } catch (NumberFormatException e) {
	            build = null;
	        }

	        if (build != null) {
	            directory.setString(ReconyxHyperFire2MakernoteDirectory.TAG_FIRMWARE_VERSION, String.format("%d.%d.%d.%s", major, minor, revision, build));
	        } else {
	            directory.setString(ReconyxHyperFire2MakernoteDirectory.TAG_FIRMWARE_VERSION, String.format("%d.%d.%d", major, minor, revision));
	            directory.addError("Error processing Reconyx HyperFire 2 makernote data: build '" + buildYearAndDate + "' is not in the expected format and will be omitted from Firmware Version.");
	        }

	        directory.setIntArray(ReconyxHyperFire2MakernoteDirectory.TAG_SEQUENCE,
	                      new int[]
	                      {
	                          reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_SEQUENCE),
	                          reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_SEQUENCE + 2)
	                      });

	        int eventNumberHigh = reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_EVENT_NUMBER);
	        int eventNumberLow = reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_EVENT_NUMBER + 2);
	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_EVENT_NUMBER, (eventNumberHigh << 16) + eventNumberLow);

	        int seconds = reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_DATE_TIME_ORIGINAL);
	        int minutes = reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 2);
	        int hour = reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 4);
	        int month = reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 6);
	        int day = reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 8);
	        int year = reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 10);

	        if ((seconds >= 0 && seconds < 60) &&
	            (minutes >= 0 && minutes < 60) &&
	            (hour >= 0 && hour < 24) &&
	            (month >= 1 && month < 13) &&
	            (day >= 1 && day < 32) &&
	            (year >= 1 && year <= 9999)) {
	            directory.setString(ReconyxHyperFire2MakernoteDirectory.TAG_DATE_TIME_ORIGINAL,
	                    String.format("%4d:%2d:%2d %2d:%2d:%2d", year, month, day, hour, minutes, seconds));
	        } else {
	            directory.addError("Error processing Reconyx HyperFire 2 makernote data: Date/Time Original " + year + "-" + month + "-" + day + " " + hour + ":" + minutes + ":" + seconds + " is not a valid date/time.");
	        }

	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_MOON_PHASE, reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_MOON_PHASE));
	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_AMBIENT_TEMPERATURE_FAHRENHEIT, reader.getInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_AMBIENT_TEMPERATURE_FAHRENHEIT));
	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_AMBIENT_TEMPERATURE, reader.getInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_AMBIENT_TEMPERATURE));

	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_CONTRAST, reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_CONTRAST));
	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_BRIGHTNESS, reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_BRIGHTNESS));
	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_SHARPNESS, reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_SHARPNESS));
	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_SATURATION, reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_SATURATION));
	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_FLASH, reader.getByte(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_FLASH));
	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_AMBIENT_INFRARED, reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_AMBIENT_INFRARED));
	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_AMBIENT_LIGHT, reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_AMBIENT_LIGHT));
	        directory.setInt(ReconyxHyperFire2MakernoteDirectory.TAG_MOTION_SENSITIVITY, reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_MOTION_SENSITIVITY));
	        directory.setDouble(ReconyxHyperFire2MakernoteDirectory.TAG_BATTERY_VOLTAGE, reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_BATTERY_VOLTAGE) / 1000.0);
	        directory.setDouble(ReconyxHyperFire2MakernoteDirectory.TAG_BATTERY_VOLTAGE_AVG, reader.getUInt16(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_BATTERY_VOLTAGE_AVG) / 1000.0);


	        directory.setString(ReconyxHyperFireMakernoteDirectory.TAG_USER_LABEL, reader.getNullTerminatedString(makernoteOffset + ReconyxHyperFireMakernoteDirectory.TAG_USER_LABEL, 44, Charsets.UTF_8));
	        directory.setStringValue(ReconyxHyperFire2MakernoteDirectory.TAG_SERIAL_NUMBER, new StringValue(reader.getBytes(makernoteOffset + ReconyxHyperFire2MakernoteDirectory.TAG_SERIAL_NUMBER, 28), Charsets.UTF_16LE));
	        // two unread bytes: the serial number's terminating null
	    } 
	    */

	   /*
	    private void processReconyxUltraFireMakernote(@NotNull final ReconyxUltraFireMakernoteDirectory directory, final int makernoteOffset, @NotNull final RandomAccessReader reader) throws IOException
	    {
	        directory.setString(ReconyxUltraFireMakernoteDirectory.TAG_LABEL, reader.getString(makernoteOffset, 9, Charsets.UTF_8));
	        */
	        /*uint makernoteID = ByteConvert.FromBigEndianToNative(reader.GetUInt32(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagMakernoteID));
	        directory.Set(ReconyxUltraFireMakernoteDirectory.TagMakernoteID, makernoteID);
	        if (makernoteID != ReconyxUltraFireMakernoteDirectory.MAKERNOTE_ID) {
	            directory.addError("Error processing Reconyx UltraFire makernote data: unknown Makernote ID 0x" + makernoteID.ToString("x8"));
	            return;
	        }
	        directory.Set(ReconyxUltraFireMakernoteDirectory.TagMakernoteSize, ByteConvert.FromBigEndianToNative(reader.GetUInt32(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagMakernoteSize)));
	        uint makernotePublicID = ByteConvert.FromBigEndianToNative(reader.GetUInt32(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagMakernotePublicID));
	        directory.Set(ReconyxUltraFireMakernoteDirectory.TagMakernotePublicID, makernotePublicID);
	        if (makernotePublicID != ReconyxUltraFireMakernoteDirectory.MAKERNOTE_PUBLIC_ID) {
	            directory.addError("Error processing Reconyx UltraFire makernote data: unknown Makernote Public ID 0x" + makernotePublicID.ToString("x8"));
	            return;
	        }*/
	    /*
	        //directory.Set(ReconyxUltraFireMakernoteDirectory.TagMakernotePublicSize, ByteConvert.FromBigEndianToNative(reader.GetUInt16(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagMakernotePublicSize)));

	        //directory.Set(ReconyxUltraFireMakernoteDirectory.TagCameraVersion, ProcessReconyxUltraFireVersion(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagCameraVersion, reader));
	        //directory.Set(ReconyxUltraFireMakernoteDirectory.TagUibVersion, ProcessReconyxUltraFireVersion(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagUibVersion, reader));
	        //directory.Set(ReconyxUltraFireMakernoteDirectory.TagBtlVersion, ProcessReconyxUltraFireVersion(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagBtlVersion, reader));
	        //directory.Set(ReconyxUltraFireMakernoteDirectory.TagPexVersion, ProcessReconyxUltraFireVersion(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagPexVersion, reader));

	        directory.setString(ReconyxUltraFireMakernoteDirectory.TAG_EVENT_TYPE, reader.getString(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_EVENT_TYPE, 1, Charsets.UTF_8));
	        directory.setIntArray(ReconyxUltraFireMakernoteDirectory.TAG_SEQUENCE,
	                      new int[]
	                      {
	                          reader.getByte(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_SEQUENCE),
	                          reader.getByte(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_SEQUENCE + 1)
	                      });
	        //directory.Set(ReconyxUltraFireMakernoteDirectory.TagEventNumber, ByteConvert.FromBigEndianToNative(reader.GetUInt32(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagEventNumber)));

	        byte seconds = reader.getByte(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL);
	        byte minutes = reader.getByte(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 1);
	        byte hour = reader.getByte(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 2);
	        byte day = reader.getByte(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 3);
	        byte month = reader.getByte(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL + 4);
	        */
	        /*ushort year = ByteConvert.FromBigEndianToNative(reader.GetUInt16(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagDateTimeOriginal + 5));
	        if ((seconds >= 0 && seconds < 60) &&
	            (minutes >= 0 && minutes < 60) &&
	            (hour >= 0 && hour < 24) &&
	            (month >= 1 && month < 13) &&
	            (day >= 1 && day < 32) &&
	            (year >= 1 && year <= 9999)) {
	            directory.Set(ReconyxUltraFireMakernoteDirectory.TAG_DATE_TIME_ORIGINAL, new DateTime(year, month, day, hour, minutes, seconds, DateTimeKind.Unspecified));
	        } else {
	            directory.addError("Error processing Reconyx UltraFire makernote data: Date/Time Original " + year + "-" + month + "-" + day + " " + hour + ":" + minutes + ":" + seconds + " is not a valid date/time.");
	        }*/
	        /*
	        //directory.Set(ReconyxUltraFireMakernoteDirectory.TagDayOfWeek, reader.GetByte(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagDayOfWeek));

	        directory.setInt(ReconyxUltraFireMakernoteDirectory.TAG_MOON_PHASE, reader.getByte(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_MOON_PHASE));
	        //directory.Set(ReconyxUltraFireMakernoteDirectory.TagAmbientTemperatureFahrenheit, ByteConvert.FromBigEndianToNative(reader.GetInt16(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagAmbientTemperatureFahrenheit)));
	        //directory.Set(ReconyxUltraFireMakernoteDirectory.TagAmbientTemperature, ByteConvert.FromBigEndianToNative(reader.GetInt16(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagAmbientTemperature)));

	        directory.setInt(ReconyxUltraFireMakernoteDirectory.TAG_FLASH, reader.getByte(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_FLASH));
	        //directory.Set(ReconyxUltraFireMakernoteDirectory.TagBatteryVoltage, ByteConvert.FromBigEndianToNative(reader.GetUInt16(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TagBatteryVoltage)) / 1000.0);
	        directory.setStringValue(ReconyxUltraFireMakernoteDirectory.TAG_SERIAL_NUMBER, new StringValue(reader.getBytes(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_SERIAL_NUMBER, 14), Charsets.UTF_8));
	        // unread byte: the serial number's terminating null
	        directory.setString(ReconyxUltraFireMakernoteDirectory.TAG_USER_LABEL, reader.getNullTerminatedString(makernoteOffset + ReconyxUltraFireMakernoteDirectory.TAG_USER_LABEL, 20, Charsets.UTF_8));
	    }  */
	} 


	
	/**
	 * Decodes Exif binary data, populating a {@link Metadata} object with tag values in {@link ExifSubIFDDirectory},
	 * {@link ExifThumbnailDirectory}, {@link ExifInteropDirectory}, {@link GpsDirectory} and one of the many camera
	 * makernote directories.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifReader implements JpegSegmentMetadataReader
	{
	    /** Exif data stored in JPEG files' APP1 segment are preceded by this six character preamble "Exif\0\0". */
	    public static final String JPEG_SEGMENT_PREAMBLE = "Exif\0\0";

	    @NotNull
	    public Iterable<JpegSegmentType> getSegmentTypes()
	    {
	        return Collections.singletonList(JpegSegmentType.APP1);
	    }

	    public void readJpegSegments(@NotNull final Iterable<byte[]> segments, @NotNull final Metadata metadata, @NotNull final JpegSegmentType segmentType)
	    {
	        assert(segmentType == JpegSegmentType.APP1);

	        for (byte[] segmentBytes : segments) {
	            // Segment must have the expected preamble
	            if (startsWithJpegExifPreamble(segmentBytes)) {
	                extract(new ByteArrayReader(segmentBytes), metadata, JPEG_SEGMENT_PREAMBLE.length());
	            }
	        }
	    }

	    /** Indicates whether 'bytes' starts with 'JpegSegmentPreamble'. */
	    public boolean startsWithJpegExifPreamble(byte[] bytes)
	    {
	        return bytes.length >= JPEG_SEGMENT_PREAMBLE.length() &&
	            new String(bytes, 0, JPEG_SEGMENT_PREAMBLE.length()).equals(JPEG_SEGMENT_PREAMBLE);
	    }

	    /** Reads TIFF formatted Exif data from start of the specified {@link RandomAccessReader}. */
	    public void extract(@NotNull final RandomAccessReader reader, @NotNull final Metadata metadata)
	    {
	        extract(reader, metadata, 0);
	    }

	    /** Reads TIFF formatted Exif data a specified offset within a {@link RandomAccessReader}. */
	    public void extract(@NotNull final RandomAccessReader reader, @NotNull final Metadata metadata, int readerOffset)
	    {
	        extract(reader, metadata, readerOffset, null);
	    }

	    /** Reads TIFF formatted Exif data at a specified offset within a {@link RandomAccessReader}. */
	    public void extract(@NotNull final RandomAccessReader reader, @NotNull final Metadata metadata, int readerOffset, @Nullable Directory parentDirectory)
	    {
	        ExifTiffHandler exifTiffHandler = new ExifTiffHandler(metadata, parentDirectory);

	        try {
	            // Read the TIFF-formatted Exif data
	            new TiffReader().processTiff(
	                reader,
	                exifTiffHandler,
	                readerOffset
	            );
	        } catch (TiffProcessingException e) {
	            exifTiffHandler.error("Exception processing TIFF data: " + e.getMessage());
	        } catch (IOException e) {
	            exifTiffHandler.error("Exception processing TIFF data: " + e.getMessage());
	        }
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link PanasonicRawIFD0Directory}.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class PanasonicRawIFD0Descriptor extends TagDescriptor<PanasonicRawIFD0Directory>
	{
	    public PanasonicRawIFD0Descriptor(@NotNull PanasonicRawIFD0Directory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType)
	        {
	            case PanasonicRawIFD0Directory.TagPanasonicRawVersion:
	                return getVersionBytesDescription(PanasonicRawIFD0Directory.TagPanasonicRawVersion, 2);
	            case PanasonicRawIFD0Directory.TagOrientation:
	                return getOrientationDescription(PanasonicRawIFD0Directory.TagOrientation);
	            default:
	                return super.getDescription(tagType);
	        }
	    }
	}


	/**
	 * These tags are found in IFD0 of Panasonic/Leica RAW, RW2 and RWL images.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class PanasonicRawIFD0Directory extends Directory
	{
	    public static final int TagPanasonicRawVersion = 0x0001;
	    public static final int TagSensorWidth = 0x0002;
	    public static final int TagSensorHeight = 0x0003;
	    public static final int TagSensorTopBorder = 0x0004;
	    public static final int TagSensorLeftBorder = 0x0005;
	    public static final int TagSensorBottomBorder = 0x0006;
	    public static final int TagSensorRightBorder = 0x0007;

	    public static final int TagBlackLevel1 = 0x0008;
	    public static final int TagBlackLevel2 = 0x0009;
	    public static final int TagBlackLevel3 = 0x000a;
	    public static final int TagLinearityLimitRed = 0x000e;
	    public static final int TagLinearityLimitGreen = 0x000f;
	    public static final int TagLinearityLimitBlue = 0x0010;
	    public static final int TagRedBalance = 0x0011;
	    public static final int TagBlueBalance = 0x0012;
	    public static final int TagWbInfo = 0x0013;

	    public static final int TagIso = 0x0017;
	    public static final int TagHighIsoMultiplierRed = 0x0018;
	    public static final int TagHighIsoMultiplierGreen = 0x0019;
	    public static final int TagHighIsoMultiplierBlue = 0x001a;
	    public static final int TagBlackLevelRed = 0x001c;
	    public static final int TagBlackLevelGreen = 0x001d;
	    public static final int TagBlackLevelBlue = 0x001e;
	    public static final int TagWbRedLevel = 0x0024;
	    public static final int TagWbGreenLevel = 0x0025;
	    public static final int TagWbBlueLevel = 0x0026;

	    public static final int TagWbInfo2 = 0x0027;

	    public static final int TagJpgFromRaw = 0x002e;

	    public static final int TagCropTop = 0x002f;
	    public static final int TagCropLeft = 0x0030;
	    public static final int TagCropBottom = 0x0031;
	    public static final int TagCropRight = 0x0032;

	    public static final int TagMake = 0x010f;
	    public static final int TagModel = 0x0110;
	    public static final int TagStripOffsets = 0x0111;
	    public static final int TagOrientation = 0x0112;
	    public static final int TagRowsPerStrip = 0x0116;
	    public static final int TagStripByteCounts = 0x0117;
	    public static final int TagRawDataOffset = 0x0118;

	    public static final int TagDistortionInfo = 0x0119;

	    public PanasonicRawIFD0Directory()
	    {
	        this.setDescriptor(new PanasonicRawIFD0Descriptor(this));
	    }

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	   
	    {
	        _tagNameMap.put(TagPanasonicRawVersion, "Panasonic Raw Version");
	        _tagNameMap.put(TagSensorWidth, "Sensor Width");
	        _tagNameMap.put(TagSensorHeight, "Sensor Height");
	        _tagNameMap.put(TagSensorTopBorder, "Sensor Top Border");
	        _tagNameMap.put(TagSensorLeftBorder, "Sensor Left Border");
	        _tagNameMap.put(TagSensorBottomBorder, "Sensor Bottom Border");
	        _tagNameMap.put(TagSensorRightBorder, "Sensor Right Border");

	        _tagNameMap.put(TagBlackLevel1, "Black Level 1");
	        _tagNameMap.put(TagBlackLevel2, "Black Level 2");
	        _tagNameMap.put(TagBlackLevel3, "Black Level 3");
	        _tagNameMap.put(TagLinearityLimitRed, "Linearity Limit Red");
	        _tagNameMap.put(TagLinearityLimitGreen, "Linearity Limit Green");
	        _tagNameMap.put(TagLinearityLimitBlue, "Linearity Limit Blue");
	        _tagNameMap.put(TagRedBalance, "Red Balance");
	        _tagNameMap.put(TagBlueBalance, "Blue Balance");

	        _tagNameMap.put(TagIso, "ISO");
	        _tagNameMap.put(TagHighIsoMultiplierRed, "High ISO Multiplier Red");
	        _tagNameMap.put(TagHighIsoMultiplierGreen, "High ISO Multiplier Green");
	        _tagNameMap.put(TagHighIsoMultiplierBlue, "High ISO Multiplier Blue");
	        _tagNameMap.put(TagBlackLevelRed, "Black Level Red");
	        _tagNameMap.put(TagBlackLevelGreen, "Black Level Green");
	        _tagNameMap.put(TagBlackLevelBlue, "Black Level Blue");
	        _tagNameMap.put(TagWbRedLevel, "WB Red Level");
	        _tagNameMap.put(TagWbGreenLevel, "WB Green Level");
	        _tagNameMap.put(TagWbBlueLevel, "WB Blue Level");

	        _tagNameMap.put(TagJpgFromRaw, "Jpg From Raw");

	        _tagNameMap.put(TagCropTop, "Crop Top");
	        _tagNameMap.put(TagCropLeft, "Crop Left");
	        _tagNameMap.put(TagCropBottom, "Crop Bottom");
	        _tagNameMap.put(TagCropRight, "Crop Right");

	        _tagNameMap.put(TagMake, "Make");
	        _tagNameMap.put(TagModel, "Model");
	        _tagNameMap.put(TagStripOffsets, "Strip Offsets");
	        _tagNameMap.put(TagOrientation, "Orientation");
	        _tagNameMap.put(TagRowsPerStrip, "Rows Per Strip");
	        _tagNameMap.put(TagStripByteCounts, "Strip Byte Counts");
	        _tagNameMap.put(TagRawDataOffset, "Raw Data Offset");
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "PanasonicRaw Exif IFD0";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Describes Exif tags from the SubIFD directory.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifSubIFDDirectory extends ExifDirectoryBase
	{
	    /** This tag is a pointer to the Exif Interop IFD. */
	    public static final int TAG_INTEROP_OFFSET = 0xA005;

	    public ExifSubIFDDirectory()
	    {
	        this.setDescriptor(new ExifSubIFDDescriptor(this));
	    }

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    
	    {
	        addExifTagNames(_tagNameMap);
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Exif SubIFD";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }

	    /**
	     * Parses the date/time tag, the subsecond tag and the time offset tag to obtain a single Date
	     * object with milliseconds representing the date and time when this image was modified.  If
	     * the time offset tag does not exist, attempts will be made to parse the values as though it is
	     * in the GMT {@link TimeZone}.
	     *
	     * @return A Date object representing when this image was modified, if possible, otherwise null
	     */
	    @Nullable
	    public Date getDateModified()
	    {
	        return getDateModified(null);
	    }

	    /**
	     * Parses the date/time tag, the subsecond tag and the time offset tag to obtain a single Date
	     * object with milliseconds representing the date and time when this image was modified.  If
	     * the time offset tag does not exist, attempts will be made to parse the values as though it is
	     * in the {@link TimeZone} represented by the {@code timeZone} parameter (if it is non-null).
	     *
	     * @param timeZone the time zone to use
	     * @return A Date object representing when this image was modified, if possible, otherwise null
	     */
	    @Nullable
	    public Date getDateModified(@Nullable TimeZone timeZone)
	    {
	        Directory parent = getParent();
	        if (parent instanceof ExifIFD0Directory) {
	            TimeZone timeZoneModified = getTimeZone(TAG_TIME_ZONE);
	            return parent.getDate(TAG_DATETIME, getString(TAG_SUBSECOND_TIME),
	                (timeZoneModified != null) ? timeZoneModified : timeZone);
	        } else {
	            return null;
	        }
	    }

	    /**
	     * Parses the date/time tag, the subsecond tag and the time offset tag to obtain a single Date
	     * object with milliseconds representing the date and time when this image was captured.  If
	     * the time offset tag does not exist, attempts will be made to parse the values as though it is
	     * in the GMT {@link TimeZone}.
	     *
	     * @return A Date object representing when this image was captured, if possible, otherwise null
	     */
	    @Nullable
	    public Date getDateOriginal()
	    {
	        return getDateOriginal(null);
	    }

	    /**
	     * Parses the date/time tag, the subsecond tag and the time offset tag to obtain a single Date
	     * object with milliseconds representing the date and time when this image was captured.  If
	     * the time offset tag does not exist, attempts will be made to parse the values as though it is
	     * in the {@link TimeZone} represented by the {@code timeZone} parameter (if it is non-null).
	     *
	     * @param timeZone the time zone to use
	     * @return A Date object representing when this image was captured, if possible, otherwise null
	     */
	    @Nullable
	    public Date getDateOriginal(@Nullable TimeZone timeZone)
	    {
	        TimeZone timeZoneOriginal = getTimeZone(TAG_TIME_ZONE_ORIGINAL);
	        return getDate(TAG_DATETIME_ORIGINAL, getString(TAG_SUBSECOND_TIME_ORIGINAL),
	            (timeZoneOriginal != null) ? timeZoneOriginal : timeZone);
	    }

	    /**
	     * Parses the date/time tag, the subsecond tag and the time offset tag to obtain a single Date
	     * object with milliseconds representing the date and time when this image was digitized.  If
	     * the time offset tag does not exist, attempts will be made to parse the values as though it is
	     * in the GMT {@link TimeZone}.
	     *
	     * @return A Date object representing when this image was digitized, if possible, otherwise null
	     */
	    @Nullable
	    public Date getDateDigitized()
	    {
	        return getDateDigitized(null);
	    }

	    /**
	     * Parses the date/time tag, the subsecond tag and the time offset tag to obtain a single Date
	     * object with milliseconds representing the date and time when this image was digitized.  If
	     * the time offset tag does not exist, attempts will be made to parse the values as though it is
	     * in the {@link TimeZone} represented by the {@code timeZone} parameter (if it is non-null).
	     *
	     * @param timeZone the time zone to use
	     * @return A Date object representing when this image was digitized, if possible, otherwise null
	     */
	    @Nullable
	    public Date getDateDigitized(@Nullable TimeZone timeZone)
	    {
	        TimeZone timeZoneDigitized = getTimeZone(TAG_TIME_ZONE_DIGITIZED);
	        return getDate(TAG_DATETIME_DIGITIZED, getString(TAG_SUBSECOND_TIME_DIGITIZED),
	            (timeZoneDigitized != null) ? timeZoneDigitized : timeZone);
	    }

	    @Nullable
	    private TimeZone getTimeZone(int tagType)
	    {
	        String timeOffset = getString(tagType);
	        if (timeOffset != null && timeOffset.matches("[\\+\\-]\\d\\d:\\d\\d")) {
	            return TimeZone.getTimeZone("GMT" + timeOffset);
	        } else {
	            return null;
	        }
	    }
	}

	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link ExifSubIFDDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifSubIFDDescriptor extends ExifDescriptorBase<ExifSubIFDDirectory>
	{
	    public ExifSubIFDDescriptor(@NotNull ExifSubIFDDirectory directory)
	    {
	        super(directory);
	    }
	}

	/**
	 * Describes Exif tags that contain Global Positioning System (GPS) data.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class GpsDirectory extends ExifDirectoryBase
	{
	    /** GPS tag version GPSVersionID 0 0 BYTE 4 */
	    public static final int TAG_VERSION_ID = 0x0000;
	    /** North or South Latitude GPSLatitudeRef 1 1 ASCII 2 */
	    public static final int TAG_LATITUDE_REF = 0x0001;
	    /** Latitude GPSLatitude 2 2 RATIONAL 3 */
	    public static final int TAG_LATITUDE = 0x0002;
	    /** East or West Longitude GPSLongitudeRef 3 3 ASCII 2 */
	    public static final int TAG_LONGITUDE_REF = 0x0003;
	    /** Longitude GPSLongitude 4 4 RATIONAL 3 */
	    public static final int TAG_LONGITUDE = 0x0004;
	    /** Altitude reference GPSAltitudeRef 5 5 BYTE 1 */
	    public static final int TAG_ALTITUDE_REF = 0x0005;
	    /** Altitude GPSAltitude 6 6 RATIONAL 1 */
	    public static final int TAG_ALTITUDE = 0x0006;
	    /** GPS time (atomic clock) GPSTimeStamp 7 7 RATIONAL 3 */
	    public static final int TAG_TIME_STAMP = 0x0007;
	    /** GPS satellites used for measurement GPSSatellites 8 8 ASCII Any */
	    public static final int TAG_SATELLITES = 0x0008;
	    /** GPS receiver status GPSStatus 9 9 ASCII 2 */
	    public static final int TAG_STATUS = 0x0009;
	    /** GPS measurement mode GPSMeasureMode 10 A ASCII 2 */
	    public static final int TAG_MEASURE_MODE = 0x000A;
	    /** Measurement precision GPSDOP 11 B RATIONAL 1 */
	    public static final int TAG_DOP = 0x000B;
	    /** Speed unit GPSSpeedRef 12 C ASCII 2 */
	    public static final int TAG_SPEED_REF = 0x000C;
	    /** Speed of GPS receiver GPSSpeed 13 D RATIONAL 1 */
	    public static final int TAG_SPEED = 0x000D;
	    /** Reference for direction of movement GPSTrackRef 14 E ASCII 2 */
	    public static final int TAG_TRACK_REF = 0x000E;
	    /** Direction of movement GPSTrack 15 F RATIONAL 1 */
	    public static final int TAG_TRACK = 0x000F;
	    /** Reference for direction of image GPSImgDirectionRef 16 10 ASCII 2 */
	    public static final int TAG_IMG_DIRECTION_REF = 0x0010;
	    /** Direction of image GPSImgDirection 17 11 RATIONAL 1 */
	    public static final int TAG_IMG_DIRECTION = 0x0011;
	    /** Geodetic survey data used GPSMapDatum 18 12 ASCII Any */
	    public static final int TAG_MAP_DATUM = 0x0012;
	    /** Reference for latitude of destination GPSDestLatitudeRef 19 13 ASCII 2 */
	    public static final int TAG_DEST_LATITUDE_REF = 0x0013;
	    /** Latitude of destination GPSDestLatitude 20 14 RATIONAL 3 */
	    public static final int TAG_DEST_LATITUDE = 0x0014;
	    /** Reference for longitude of destination GPSDestLongitudeRef 21 15 ASCII 2 */
	    public static final int TAG_DEST_LONGITUDE_REF = 0x0015;
	    /** Longitude of destination GPSDestLongitude 22 16 RATIONAL 3 */
	    public static final int TAG_DEST_LONGITUDE = 0x0016;
	    /** Reference for bearing of destination GPSDestBearingRef 23 17 ASCII 2 */
	    public static final int TAG_DEST_BEARING_REF = 0x0017;
	    /** Bearing of destination GPSDestBearing 24 18 RATIONAL 1 */
	    public static final int TAG_DEST_BEARING = 0x0018;
	    /** Reference for distance to destination GPSDestDistanceRef 25 19 ASCII 2 */
	    public static final int TAG_DEST_DISTANCE_REF = 0x0019;
	    /** Distance to destination GPSDestDistance 26 1A RATIONAL 1 */
	    public static final int TAG_DEST_DISTANCE = 0x001A;
	    /** Name of the method used for location finding GPSProcessingMethod 27 1B UNDEFINED Any */
	    public static final int TAG_PROCESSING_METHOD = 0x001B;
	    /** Name of the GPS area GPSAreaInformation 28 1C UNDEFINED Any */
	    public static final int TAG_AREA_INFORMATION = 0x001C;
	    /** Date and time GPSDateStamp 29 1D ASCII 11 */
	    public static final int TAG_DATE_STAMP = 0x001D;
	    /** Whether differential correction is applied GPSDifferential 30 1E SHORT 1 */
	    public static final int TAG_DIFFERENTIAL = 0x001E;
	    /** Horizontal positioning errors GPSHPositioningError 31 1F RATIONAL 1 */
	    public static final int TAG_H_POSITIONING_ERROR = 0x001F;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    {
	        addExifTagNames(_tagNameMap);

	        _tagNameMap.put(TAG_VERSION_ID, "GPS Version ID");
	        _tagNameMap.put(TAG_LATITUDE_REF, "GPS Latitude Ref");
	        _tagNameMap.put(TAG_LATITUDE, "GPS Latitude");
	        _tagNameMap.put(TAG_LONGITUDE_REF, "GPS Longitude Ref");
	        _tagNameMap.put(TAG_LONGITUDE, "GPS Longitude");
	        _tagNameMap.put(TAG_ALTITUDE_REF, "GPS Altitude Ref");
	        _tagNameMap.put(TAG_ALTITUDE, "GPS Altitude");
	        _tagNameMap.put(TAG_TIME_STAMP, "GPS Time-Stamp");
	        _tagNameMap.put(TAG_SATELLITES, "GPS Satellites");
	        _tagNameMap.put(TAG_STATUS, "GPS Status");
	        _tagNameMap.put(TAG_MEASURE_MODE, "GPS Measure Mode");
	        _tagNameMap.put(TAG_DOP, "GPS DOP");
	        _tagNameMap.put(TAG_SPEED_REF, "GPS Speed Ref");
	        _tagNameMap.put(TAG_SPEED, "GPS Speed");
	        _tagNameMap.put(TAG_TRACK_REF, "GPS Track Ref");
	        _tagNameMap.put(TAG_TRACK, "GPS Track");
	        _tagNameMap.put(TAG_IMG_DIRECTION_REF, "GPS Img Direction Ref");
	        _tagNameMap.put(TAG_IMG_DIRECTION, "GPS Img Direction");
	        _tagNameMap.put(TAG_MAP_DATUM, "GPS Map Datum");
	        _tagNameMap.put(TAG_DEST_LATITUDE_REF, "GPS Dest Latitude Ref");
	        _tagNameMap.put(TAG_DEST_LATITUDE, "GPS Dest Latitude");
	        _tagNameMap.put(TAG_DEST_LONGITUDE_REF, "GPS Dest Longitude Ref");
	        _tagNameMap.put(TAG_DEST_LONGITUDE, "GPS Dest Longitude");
	        _tagNameMap.put(TAG_DEST_BEARING_REF, "GPS Dest Bearing Ref");
	        _tagNameMap.put(TAG_DEST_BEARING, "GPS Dest Bearing");
	        _tagNameMap.put(TAG_DEST_DISTANCE_REF, "GPS Dest Distance Ref");
	        _tagNameMap.put(TAG_DEST_DISTANCE, "GPS Dest Distance");
	        _tagNameMap.put(TAG_PROCESSING_METHOD, "GPS Processing Method");
	        _tagNameMap.put(TAG_AREA_INFORMATION, "GPS Area Information");
	        _tagNameMap.put(TAG_DATE_STAMP, "GPS Date Stamp");
	        _tagNameMap.put(TAG_DIFFERENTIAL, "GPS Differential");
	        _tagNameMap.put(TAG_H_POSITIONING_ERROR, "GPS Horizontal Positioning Error");
	    }

	    public GpsDirectory()
	    {
	        this.setDescriptor(new GpsDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "GPS";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }

	    /**
	     * Parses various tags in an attempt to obtain a single object representing the latitude and longitude
	     * at which this image was captured.
	     *
	     * @return The geographical location of this image, if possible, otherwise null
	     */
	    @Nullable
	    public GeoLocation getGeoLocation()
	    {
	        Rational[] latitudes = getRationalArray(TAG_LATITUDE);
	        Rational[] longitudes = getRationalArray(TAG_LONGITUDE);
	        String latitudeRef = getString(TAG_LATITUDE_REF);
	        String longitudeRef = getString(TAG_LONGITUDE_REF);

	        // Make sure we have the required values
	        if (latitudes == null || latitudes.length != 3)
	            return null;
	        if (longitudes == null || longitudes.length != 3)
	            return null;
	        if (latitudeRef == null || longitudeRef == null)
	            return null;
	        
	        GeoLocation geo = new GeoLocation();

	        Double lat = geo.degreesMinutesSecondsToDecimal(latitudes[0], latitudes[1], latitudes[2], latitudeRef.equalsIgnoreCase("S"));
	        Double lon = geo.degreesMinutesSecondsToDecimal(longitudes[0], longitudes[1], longitudes[2], longitudeRef.equalsIgnoreCase("W"));

	        // This can return null, in cases where the conversion was not possible
	        if (lat == null || lon == null)
	            return null;

	        return new GeoLocation(lat, lon);
	    }

	    /**
	     * Parses the date stamp tag and the time stamp tag to obtain a single Date object representing the
	     * date and time when this image was captured.
	     *
	     * @return A Date object representing when this image was captured, if possible, otherwise null
	     */
	    @Nullable
	    public Date getGpsDate()
	    {
	        String date = getString(TAG_DATE_STAMP);
	        Rational[] timeComponents = getRationalArray(TAG_TIME_STAMP);

	        // Make sure we have the required values
	        if (date == null)
	            return null;
	        if (timeComponents == null || timeComponents.length != 3)
	            return null;

	        String dateTime = String.format(Locale.US, "%s %02d:%02d:%02.3f UTC",
	            date, timeComponents[0].intValue(), timeComponents[1].intValue(), timeComponents[2].doubleValue());
	        try {
	            DateFormat parser = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss.S z");
	            return parser.parse(dateTime);
	        } catch (ParseException e) {
	            return null;
	        }
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link GpsDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class GpsDescriptor extends TagDescriptor<GpsDirectory>
	{
	    public GpsDescriptor(@NotNull GpsDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case GpsDirectory.TAG_VERSION_ID:
	                return getGpsVersionIdDescription();
	            case GpsDirectory.TAG_ALTITUDE:
	                return getGpsAltitudeDescription();
	            case GpsDirectory.TAG_ALTITUDE_REF:
	                return getGpsAltitudeRefDescription();
	            case GpsDirectory.TAG_STATUS:
	                return getGpsStatusDescription();
	            case GpsDirectory.TAG_MEASURE_MODE:
	                return getGpsMeasureModeDescription();
	            case GpsDirectory.TAG_DOP:
	                return getGpsDopDescription();
	            case GpsDirectory.TAG_SPEED_REF:
	                return getGpsSpeedRefDescription();
	            case GpsDirectory.TAG_SPEED:
	                return getGpsSpeedDescription();
	            case GpsDirectory.TAG_TRACK_REF:
	            case GpsDirectory.TAG_IMG_DIRECTION_REF:
	            case GpsDirectory.TAG_DEST_BEARING_REF:
	                return getGpsDirectionReferenceDescription(tagType);
	            case GpsDirectory.TAG_TRACK:
	            case GpsDirectory.TAG_IMG_DIRECTION:
	            case GpsDirectory.TAG_DEST_BEARING:
	                return getGpsDirectionDescription(tagType);
	            case GpsDirectory.TAG_DEST_LATITUDE:
	                return getGpsDestLatitudeDescription();
	            case GpsDirectory.TAG_DEST_LONGITUDE:
	                return getGpsDestLongitudeDescription();
	            case GpsDirectory.TAG_DEST_DISTANCE_REF:
	                return getGpsDestinationReferenceDescription();
	            case GpsDirectory.TAG_DEST_DISTANCE:
	                return getGpsDestDistanceDescription();
	            case GpsDirectory.TAG_TIME_STAMP:
	                return getGpsTimeStampDescription();
	            case GpsDirectory.TAG_LONGITUDE:
	                // three rational numbers -- displayed in HH"MM"SS.ss
	                return getGpsLongitudeDescription();
	            case GpsDirectory.TAG_LATITUDE:
	                // three rational numbers -- displayed in HH"MM"SS.ss
	                return getGpsLatitudeDescription();
	            case GpsDirectory.TAG_PROCESSING_METHOD:
	                return getGpsProcessingMethodDescription();
	            case GpsDirectory.TAG_AREA_INFORMATION:
	                return getGpsAreaInformationDescription();
	            case GpsDirectory.TAG_DIFFERENTIAL:
	                return getGpsDifferentialDescription();
	            case GpsDirectory.TAG_H_POSITIONING_ERROR:
	                return getGpsHPositioningErrorDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    private String getGpsVersionIdDescription()
	    {
	        return getVersionBytesDescription(GpsDirectory.TAG_VERSION_ID, 1);
	    }

	    @Nullable
	    public String getGpsLatitudeDescription()
	    {
	        GeoLocation location = _directory.getGeoLocation();
	        GeoLocation geo = new GeoLocation();
	        return location == null ? null : geo.decimalToDegreesMinutesSecondsString(location.getLatitude());
	    }

	    @Nullable
	    public String getGpsLongitudeDescription()
	    {
	        GeoLocation location = _directory.getGeoLocation();
	        GeoLocation geo = new GeoLocation();
	        return location == null ? null : geo.decimalToDegreesMinutesSecondsString(location.getLongitude());
	    }

	    @Nullable
	    public String getGpsTimeStampDescription()
	    {
	        // time in hour, min, sec
	        Rational[] timeComponents = _directory.getRationalArray(GpsDirectory.TAG_TIME_STAMP);
	        DecimalFormat df = new DecimalFormat("00.000");
	        return timeComponents == null
	            ? null
	            : String.format("%02d:%02d:%s UTC",
	                timeComponents[0].intValue(),
	                timeComponents[1].intValue(),
	                df.format(timeComponents[2].doubleValue()));
	    }

	    @Nullable
	    public String getGpsDestLatitudeDescription()
	    {
	        return getGeoLocationDimension(GpsDirectory.TAG_DEST_LATITUDE, GpsDirectory.TAG_DEST_LATITUDE_REF, "S");
	    }

	    @Nullable
	    public String getGpsDestLongitudeDescription()
	    {
	        return getGeoLocationDimension(GpsDirectory.TAG_DEST_LONGITUDE, GpsDirectory.TAG_DEST_LONGITUDE_REF, "W");
	    }

	    @Nullable
	    private String getGeoLocationDimension(int tagValue, int tagRef, String positiveRef)
	    {
	        Rational[] values = _directory.getRationalArray(tagValue);
	        String ref = _directory.getString(tagRef);

	        if (values == null || values.length != 3 || ref == null)
	            return null;

	        GeoLocation geo = new GeoLocation();
	        Double dec = geo.degreesMinutesSecondsToDecimal(
	            values[0], values[1], values[2], ref.equalsIgnoreCase(positiveRef));

	        return dec == null ? null : geo.decimalToDegreesMinutesSecondsString(dec);
	    }

	    @Nullable
	    public String getGpsDestinationReferenceDescription()
	    {
	        final String value = _directory.getString(GpsDirectory.TAG_DEST_DISTANCE_REF);
	        if (value == null)
	            return null;
	        String distanceRef = value.trim();
	        if ("K".equalsIgnoreCase(distanceRef)) {
	            return "kilometers";
	        } else if ("M".equalsIgnoreCase(distanceRef)) {
	            return "miles";
	        } else if ("N".equalsIgnoreCase(distanceRef)) {
	            return "knots";
	        } else {
	            return "Unknown (" + distanceRef + ")";
	        }
	    }

	    @Nullable
	    public String getGpsDestDistanceDescription()
	    {
	        final Rational value = _directory.getRational(GpsDirectory.TAG_DEST_DISTANCE);
	        if (value == null)
	            return null;
	        final String unit = getGpsDestinationReferenceDescription();
	        return String.format("%s %s",
	            new DecimalFormat("0.##").format(value.doubleValue()),
	            unit == null ? "unit" : unit.toLowerCase());
	    }

	    @Nullable
	    public String getGpsDirectionDescription(int tagType)
	    {
	        Rational angle = _directory.getRational(tagType);
	        // provide a decimal version of rational numbers in the description, to avoid strings like "35334/199 degrees"
	        String value = angle != null
	            ? new DecimalFormat("0.##").format(angle.doubleValue())
	            : _directory.getString(tagType);
	        return value == null || value.trim().length() == 0 ? null : value.trim() + " degrees";
	    }

	    @Nullable
	    public String getGpsDirectionReferenceDescription(int tagType)
	    {
	        final String value = _directory.getString(tagType);
	        if (value == null)
	            return null;
	        String gpsDistRef = value.trim();
	        if ("T".equalsIgnoreCase(gpsDistRef)) {
	            return "True direction";
	        } else if ("M".equalsIgnoreCase(gpsDistRef)) {
	            return "Magnetic direction";
	        } else {
	            return "Unknown (" + gpsDistRef + ")";
	        }
	    }

	    @Nullable
	    public String getGpsDopDescription()
	    {
	        final Rational value = _directory.getRational(GpsDirectory.TAG_DOP);
	        return value == null ? null : new DecimalFormat("0.##").format(value.doubleValue());
	    }

	    @Nullable
	    public String getGpsSpeedRefDescription()
	    {
	        final String value = _directory.getString(GpsDirectory.TAG_SPEED_REF);
	        if (value == null)
	            return null;
	        String gpsSpeedRef = value.trim();
	        if ("K".equalsIgnoreCase(gpsSpeedRef)) {
	            return "km/h";
	        } else if ("M".equalsIgnoreCase(gpsSpeedRef)) {
	            return "mph";
	        } else if ("N".equalsIgnoreCase(gpsSpeedRef)) {
	            return "knots";
	        } else {
	            return "Unknown (" + gpsSpeedRef + ")";
	        }
	    }

	    @Nullable
	    public String getGpsSpeedDescription()
	    {
	        final Rational value = _directory.getRational(GpsDirectory.TAG_SPEED);
	        if (value == null)
	            return null;
	        final String unit = getGpsSpeedRefDescription();
	        return String.format("%s %s",
	            new DecimalFormat("0.##").format(value.doubleValue()),
	            unit == null ? "unit" : unit.toLowerCase());
	    }

	    @Nullable
	    public String getGpsMeasureModeDescription()
	    {
	        final String value = _directory.getString(GpsDirectory.TAG_MEASURE_MODE);
	        if (value == null)
	            return null;
	        String gpsSpeedMeasureMode = value.trim();
	        if ("2".equalsIgnoreCase(gpsSpeedMeasureMode)) {
	            return "2-dimensional measurement";
	        } else if ("3".equalsIgnoreCase(gpsSpeedMeasureMode)) {
	            return "3-dimensional measurement";
	        } else {
	            return "Unknown (" + gpsSpeedMeasureMode + ")";
	        }
	    }

	    @Nullable
	    public String getGpsStatusDescription()
	    {
	        final String value = _directory.getString(GpsDirectory.TAG_STATUS);
	        if (value == null)
	            return null;
	        String gpsStatus = value.trim();
	        if ("A".equalsIgnoreCase(gpsStatus)) {
	            return "Active (Measurement in progress)";
	        } else if ("V".equalsIgnoreCase(gpsStatus)) {
	            return "Void (Measurement Interoperability)";
	        } else {
	            return "Unknown (" + gpsStatus + ")";
	        }
	    }

	    @Nullable
	    public String getGpsAltitudeRefDescription()
	    {
	        return getIndexedDescription(GpsDirectory.TAG_ALTITUDE_REF, "Sea level", "Below sea level");
	    }

	    @Nullable
	    public String getGpsAltitudeDescription()
	    {
	        final Rational value = _directory.getRational(GpsDirectory.TAG_ALTITUDE);
	        return value == null ? null : new DecimalFormat("0.##").format(value.doubleValue()) + " metres";
	    }

	    @Nullable
	    public String getGpsProcessingMethodDescription()
	    {
	        return getEncodedTextDescription(GpsDirectory.TAG_PROCESSING_METHOD);
	    }

	    @Nullable
	    public String getGpsAreaInformationDescription()
	    {
	        return getEncodedTextDescription(GpsDirectory.TAG_AREA_INFORMATION);
	    }

	    @Nullable
	    public String getGpsDifferentialDescription()
	    {
	        return getIndexedDescription(GpsDirectory.TAG_DIFFERENTIAL, "No Correction", "Differential Corrected");
	    }

	    @Nullable
	    public String getGpsHPositioningErrorDescription()
	    {
	        final Rational value = _directory.getRational(GpsDirectory.TAG_H_POSITIONING_ERROR);
	        return value == null ? null : new DecimalFormat("0.##").format(value.doubleValue()) + " metres";
	    }

	    @Nullable
	    public String getDegreesMinutesSecondsDescription()
	    {
	        GeoLocation location = _directory.getGeoLocation();
	        return location == null ? null : location.toDMSString();
	    }
	}

	/**
	 * Represents a latitude and longitude pair, giving a position on earth in spherical coordinates.
	 * <p>
	 * Values of latitude and longitude are given in degrees.
	 * <p>
	 * This type is immutable.
	 */
	public final class GeoLocation
	{
	    private double _latitude;
	    private double _longitude;
	    
	    public GeoLocation() {
	    	
	    }

	    /**
	     * Instantiates a new instance of {@link GeoLocation}.
	     *
	     * @param latitude the latitude, in degrees
	     * @param longitude the longitude, in degrees
	     */
	    public GeoLocation(double latitude, double longitude)
	    {
	        _latitude = latitude;
	        _longitude = longitude;
	    }

	    /**
	     * @return the latitudinal angle of this location, in degrees.
	     */
	    public double getLatitude()
	    {
	        return _latitude;
	    }

	    /**
	     * @return the longitudinal angle of this location, in degrees.
	     */
	    public double getLongitude()
	    {
	        return _longitude;
	    }

	    /**
	     * @return true, if both latitude and longitude are equal to zero
	     */
	    public boolean isZero()
	    {
	        return _latitude == 0 && _longitude == 0;
	    }

	    /**
	     * Converts a decimal degree angle into its corresponding DMS (degrees-minutes-seconds) representation as a string,
	     * of format: {@code -1Â° 23' 4.56"}
	     */
	    @NotNull
	    public String decimalToDegreesMinutesSecondsString(double decimal)
	    {
	        double[] dms = decimalToDegreesMinutesSeconds(decimal);
	        DecimalFormat format = new DecimalFormat("0.##");
	        return String.format("%s\u00B0 %s' %s\"", format.format(dms[0]), format.format(dms[1]), format.format(dms[2]));
	    }

	    /**
	     * Converts a decimal degree angle into its corresponding DMS (degrees-minutes-seconds) component values, as
	     * a double array.
	     */
	    @NotNull
	    public double[] decimalToDegreesMinutesSeconds(double decimal)
	    {
	        int d = (int)decimal;
	        double m = Math.abs((decimal % 1) * 60);
	        double s = (m % 1) * 60;
	        return new double[] { d, (int)m, s};
	    }

	    /**
	     * Converts DMS (degrees-minutes-seconds) rational values, as given in {@link com.drew.metadata.exif.GpsDirectory},
	     * into a single value in degrees, as a double.
	     */
	    @Nullable
	    public Double degreesMinutesSecondsToDecimal(@NotNull final Rational degs, @NotNull final Rational mins, @NotNull final Rational secs, final boolean isNegative)
	    {
	        double decimal = Math.abs(degs.doubleValue())
	                + mins.doubleValue() / 60.0d
	                + secs.doubleValue() / 3600.0d;

	        if (Double.isNaN(decimal))
	            return null;

	        if (isNegative)
	            decimal *= -1;

	        return decimal;
	    }

	    @Override
	    public boolean equals(final Object o)
	    {
	        if (this == o) return true;
	        if (o == null || getClass() != o.getClass()) return false;
	        GeoLocation that = (GeoLocation) o;
	        if (Double.compare(that._latitude, _latitude) != 0) return false;
	        if (Double.compare(that._longitude, _longitude) != 0) return false;
	        return true;
	    }

	    @Override
	    public int hashCode()
	    {
	        int result;
	        long temp;
	        temp = _latitude != +0.0d ? Double.doubleToLongBits(_latitude) : 0L;
	        result = (int) (temp ^ (temp >>> 32));
	        temp = _longitude != +0.0d ? Double.doubleToLongBits(_longitude) : 0L;
	        result = 31 * result + (int) (temp ^ (temp >>> 32));
	        return result;
	    }

	    /**
	     * @return a string representation of this location, of format: {@code 1.23, 4.56}
	     */
	    @Override
	    @NotNull
	    public String toString()
	    {
	        return _latitude + ", " + _longitude;
	    }

	    /**
	     * @return a string representation of this location, of format: {@code -1Â° 23' 4.56", 54Â° 32' 1.92"}
	     */
	    @NotNull
	    public String toDMSString()
	    {
	        return decimalToDegreesMinutesSecondsString(_latitude) + ", " + decimalToDegreesMinutesSecondsString(_longitude);
	    }
	}
	
	/**
	 * Holds a set of commonly used character encodings.
	 *
	 * Newer JDKs include java.nio.charset.StandardCharsets, but we cannot use that in this library.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	public final class Charsets
	{
		public Charsets() {
			
		}
	    public final Charset UTF_8 = Charset.forName("UTF-8");
	    public final Charset UTF_16 = Charset.forName("UTF-16");
	    public final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
	    public final Charset ASCII = Charset.forName("US-ASCII");
	    public final Charset UTF_16BE = Charset.forName("UTF-16BE");
	    public final Charset UTF_16LE = Charset.forName("UTF-16LE");
	    public final Charset WINDOWS_1252 = Charset.forName("Cp1252");
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link ExifInteropDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifInteropDescriptor extends ExifDescriptorBase<ExifInteropDirectory>
	{
	    public ExifInteropDescriptor(@NotNull ExifInteropDirectory directory)
	    {
	        super(directory);
	    }
	}


	/**
	 * Describes Exif interoperability tags.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifInteropDirectory extends ExifDirectoryBase
	{
	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    {
	        addExifTagNames(_tagNameMap);
	    }

	    public ExifInteropDirectory()
	    {
	        this.setDescriptor(new ExifInteropDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Interoperability";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link OlympusMakernoteDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusMakernoteDescriptor extends TagDescriptor<OlympusMakernoteDirectory>
	{
	    // TODO extend support for some offset-encoded byte[] tags: http://www.ozhiker.com/electronics/pjmt/jpeg_info/olympus_mn.html

	    public OlympusMakernoteDescriptor(@NotNull OlympusMakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case OlympusMakernoteDirectory.TAG_MAKERNOTE_VERSION:
	                return getMakernoteVersionDescription();
	            case OlympusMakernoteDirectory.TAG_COLOUR_MODE:
	                return getColorModeDescription();
	            case OlympusMakernoteDirectory.TAG_IMAGE_QUALITY_1:
	                return getImageQuality1Description();
	            case OlympusMakernoteDirectory.TAG_IMAGE_QUALITY_2:
	                return getImageQuality2Description();
	            case OlympusMakernoteDirectory.TAG_SPECIAL_MODE:
	                return getSpecialModeDescription();
	            case OlympusMakernoteDirectory.TAG_JPEG_QUALITY:
	                return getJpegQualityDescription();
	            case OlympusMakernoteDirectory.TAG_MACRO_MODE:
	                return getMacroModeDescription();
	            case OlympusMakernoteDirectory.TAG_BW_MODE:
	                return getBWModeDescription();
	            case OlympusMakernoteDirectory.TAG_DIGITAL_ZOOM:
	                return getDigitalZoomDescription();
	            case OlympusMakernoteDirectory.TAG_FOCAL_PLANE_DIAGONAL:
	                return getFocalPlaneDiagonalDescription();
	            case OlympusMakernoteDirectory.TAG_CAMERA_TYPE:
	                return getCameraTypeDescription();
	            case OlympusMakernoteDirectory.TAG_CAMERA_ID:
	                return getCameraIdDescription();
	            case OlympusMakernoteDirectory.TAG_ONE_TOUCH_WB:
	                return getOneTouchWbDescription();
	            case OlympusMakernoteDirectory.TAG_SHUTTER_SPEED_VALUE:
	                return getShutterSpeedDescription();
	            case OlympusMakernoteDirectory.TAG_ISO_VALUE:
	                return getIsoValueDescription();
	            case OlympusMakernoteDirectory.TAG_APERTURE_VALUE:
	                return getApertureValueDescription();
	            case OlympusMakernoteDirectory.TAG_FLASH_MODE:
	                return getFlashModeDescription();
	            case OlympusMakernoteDirectory.TAG_FOCUS_RANGE:
	                return getFocusRangeDescription();
	            case OlympusMakernoteDirectory.TAG_FOCUS_MODE:
	                return getFocusModeDescription();
	            case OlympusMakernoteDirectory.TAG_SHARPNESS:
	                return getSharpnessDescription();
	            case OlympusMakernoteDirectory.TAG_COLOUR_MATRIX:
	                return getColorMatrixDescription();
	            case OlympusMakernoteDirectory.TAG_WB_MODE:
	                return getWbModeDescription();
	            case OlympusMakernoteDirectory.TAG_RED_BALANCE:
	                return getRedBalanceDescription();
	            case OlympusMakernoteDirectory.TAG_BLUE_BALANCE:
	                return getBlueBalanceDescription();
	            case OlympusMakernoteDirectory.TAG_CONTRAST:
	                return getContrastDescription();
	            case OlympusMakernoteDirectory.TAG_PREVIEW_IMAGE_VALID:
	                return getPreviewImageValidDescription();

	            case OlympusMakernoteDirectory.CameraSettings.TAG_EXPOSURE_MODE:
	                return getExposureModeDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_FLASH_MODE:
	                return getFlashModeCameraSettingDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_WHITE_BALANCE:
	                return getWhiteBalanceDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_IMAGE_SIZE:
	                return getImageSizeDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_IMAGE_QUALITY:
	                return getImageQualityDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_SHOOTING_MODE:
	                return getShootingModeDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_METERING_MODE:
	                return getMeteringModeDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_APEX_FILM_SPEED_VALUE:
	                return getApexFilmSpeedDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_APEX_SHUTTER_SPEED_TIME_VALUE:
	                return getApexShutterSpeedTimeDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_APEX_APERTURE_VALUE:
	                return getApexApertureDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_MACRO_MODE:
	                return getMacroModeCameraSettingDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_DIGITAL_ZOOM:
	                return getDigitalZoomCameraSettingDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_EXPOSURE_COMPENSATION:
	                return getExposureCompensationDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_BRACKET_STEP:
	                return getBracketStepDescription();

	            case OlympusMakernoteDirectory.CameraSettings.TAG_INTERVAL_LENGTH:
	                return getIntervalLengthDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_INTERVAL_NUMBER:
	                return getIntervalNumberDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_FOCAL_LENGTH:
	                return getFocalLengthDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_FOCUS_DISTANCE:
	                return getFocusDistanceDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_FLASH_FIRED:
	                return getFlashFiredDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_DATE:
	                return getDateDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_TIME:
	                return getTimeDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_MAX_APERTURE_AT_FOCAL_LENGTH:
	                return getMaxApertureAtFocalLengthDescription();

	            case OlympusMakernoteDirectory.CameraSettings.TAG_FILE_NUMBER_MEMORY:
	                return getFileNumberMemoryDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_LAST_FILE_NUMBER:
	                return getLastFileNumberDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_WHITE_BALANCE_RED:
	                return getWhiteBalanceRedDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_WHITE_BALANCE_GREEN:
	                return getWhiteBalanceGreenDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_WHITE_BALANCE_BLUE:
	                return getWhiteBalanceBlueDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_SATURATION:
	                return getSaturationDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_CONTRAST:
	                return getContrastCameraSettingDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_SHARPNESS:
	                return getSharpnessCameraSettingDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_SUBJECT_PROGRAM:
	                return getSubjectProgramDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_FLASH_COMPENSATION:
	                return getFlashCompensationDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_ISO_SETTING:
	                return getIsoSettingDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_CAMERA_MODEL:
	                return getCameraModelDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_INTERVAL_MODE:
	                return getIntervalModeDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_FOLDER_NAME:
	                return getFolderNameDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_COLOR_MODE:
	                return getColorModeCameraSettingDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_COLOR_FILTER:
	                return getColorFilterDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_BLACK_AND_WHITE_FILTER:
	                return getBlackAndWhiteFilterDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_INTERNAL_FLASH:
	                return getInternalFlashDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_APEX_BRIGHTNESS_VALUE:
	                return getApexBrightnessDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_SPOT_FOCUS_POINT_X_COORDINATE:
	                return getSpotFocusPointXCoordinateDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_SPOT_FOCUS_POINT_Y_COORDINATE:
	                return getSpotFocusPointYCoordinateDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_WIDE_FOCUS_ZONE:
	                return getWideFocusZoneDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_FOCUS_MODE:
	                return getFocusModeCameraSettingDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_FOCUS_AREA:
	                return getFocusAreaDescription();
	            case OlympusMakernoteDirectory.CameraSettings.TAG_DEC_SWITCH_POSITION:
	                return getDecSwitchPositionDescription();

	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getExposureModeDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_EXPOSURE_MODE, "P", "A", "S", "M");
	    }

	    @Nullable
	    public String getFlashModeCameraSettingDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_FLASH_MODE,
	            "Normal", "Red-eye reduction", "Rear flash sync", "Wireless");
	    }

	    @Nullable
	    public String getWhiteBalanceDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_WHITE_BALANCE,
	            "Auto", // 0
	            "Daylight",
	            "Cloudy",
	            "Tungsten",
	            null,
	            "Custom", // 5
	            null,
	            "Fluorescent",
	            "Fluorescent 2",
	            null,
	            null, // 10
	            "Custom 2",
	            "Custom 3"
	        );
	    }

	    @Nullable
	    public String getImageSizeDescription()
	    {
	        // This is a pretty weird way to store this information!
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_IMAGE_SIZE, "2560 x 1920", "1600 x 1200", "1280 x 960", "640 x 480");
	    }

	    @Nullable
	    public String getImageQualityDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_IMAGE_QUALITY, "Raw", "Super Fine", "Fine", "Standard", "Economy", "Extra Fine");
	    }

	    @Nullable
	    public String getShootingModeDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_SHOOTING_MODE,
	            "Single",
	            "Continuous",
	            "Self Timer",
	            null,
	            "Bracketing",
	            "Interval",
	            "UHS Continuous",
	            "HS Continuous"
	        );
	    }

	    @Nullable
	    public String getMeteringModeDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_METERING_MODE, "Multi-Segment", "Centre Weighted", "Spot");
	    }

	    @Nullable
	    public String getApexFilmSpeedDescription()
	    {
	        // http://www.ozhiker.com/electronics/pjmt/jpeg_info/minolta_mn.html#Minolta_Camera_Settings
	        // Apex Speed value = value/8-1 ,
	        // ISO = (2^(value/8-1))*3.125
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_APEX_FILM_SPEED_VALUE);

	        if (value == null)
	            return null;

	        double iso = Math.pow((value / 8d) - 1, 2) * 3.125;
	        DecimalFormat format = new DecimalFormat("0.##");
	        format.setRoundingMode(RoundingMode.HALF_UP);
	        return format.format(iso);
	    }

	    @Nullable
	    public String getApexShutterSpeedTimeDescription()
	    {
	        // http://www.ozhiker.com/electronics/pjmt/jpeg_info/minolta_mn.html#Minolta_Camera_Settings
	        // Apex Time value = value/8-6 ,
	        // ShutterSpeed = 2^( (48-value)/8 ),
	        // Due to rounding error value=8 should be displayed as 30 sec.
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_APEX_SHUTTER_SPEED_TIME_VALUE);

	        if (value == null)
	            return null;

	        double shutterSpeed = Math.pow((49-value) / 8d, 2);
	        DecimalFormat format = new DecimalFormat("0.###");
	        format.setRoundingMode(RoundingMode.HALF_UP);
	        return format.format(shutterSpeed) + " sec";
	    }

	    @Nullable
	    public String getApexApertureDescription()
	    {
	        // http://www.ozhiker.com/electronics/pjmt/jpeg_info/minolta_mn.html#Minolta_Camera_Settings
	        // Apex Aperture Value = value/8-1 ,
	        // Aperture F-stop = 2^( value/16-0.5 )
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_APEX_APERTURE_VALUE);

	        if (value == null)
	            return null;

	        double fStop = Math.pow((value/16d) - 0.5, 2);
	        return getFStopDescription(fStop);
	    }

	    @Nullable
	    public String getMacroModeCameraSettingDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_MACRO_MODE, "Off", "On");
	    }

	    @Nullable
	    public String getDigitalZoomCameraSettingDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_DIGITAL_ZOOM, "Off", "Electronic magnification", "Digital zoom 2x");
	    }

	    @Nullable
	    public String getExposureCompensationDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_EXPOSURE_COMPENSATION);
	        DecimalFormat format = new DecimalFormat("0.##");
	        return value == null ? null : format.format((value / 3d) - 2) + " EV";
	    }

	    @Nullable
	    public String getBracketStepDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_BRACKET_STEP, "1/3 EV", "2/3 EV", "1 EV");
	    }

	    @Nullable
	    public String getIntervalLengthDescription()
	    {
	        if (!_directory.isIntervalMode())
	            return "N/A";

	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_INTERVAL_LENGTH);
	        return value == null ? null : value + " min";
	    }

	    @Nullable
	    public String getIntervalNumberDescription()
	    {
	        if (!_directory.isIntervalMode())
	            return "N/A";

	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_INTERVAL_NUMBER);
	        return value == null ? null : Long.toString(value);
	    }

	    @Nullable
	    public String getFocalLengthDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_FOCAL_LENGTH);
	        return value == null ? null : getFocalLengthDescription(value/256d);
	    }

	    @Nullable
	    public String getFocusDistanceDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_FOCUS_DISTANCE);
	        return value == null
	            ? null
	            : value == 0
	                ? "Infinity"
	                : value + " mm";
	    }

	    @Nullable
	    public String getFlashFiredDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_FLASH_FIRED, "No", "Yes");
	    }

	    @Nullable
	    public String getDateDescription()
	    {
	        // day = value%256,
	        // month = floor( (value - floor( value/65536 )*65536 )/256 )
	        // year = floor( value/65536)
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_DATE);
	        if (value == null)
	            return null;

	        int day = (int) (value & 0xFF);
	        int month = (int) ((value >> 16) & 0xFF);
	        int year = (int) ((value >> 8) & 0xFF) + 1970;

	        DateUtil dt = new DateUtil();
	        if (!dt.isValidDate(year, month, day))
	            return "Invalid date";

	        return String.format("%04d-%02d-%02d", year, month + 1, day);
	    }

	    @Nullable
	    public String getTimeDescription()
	    {
	        // hours = floor( value/65536 ),
	        // minutes = floor( ( value - floor( value/65536 )*65536 )/256 ),
	        // seconds = value%256
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_TIME);
	        if (value == null)
	            return null;

	        int hours = (int) ((value >> 8) & 0xFF);
	        int minutes = (int) ((value >> 16) & 0xFF);
	        int seconds = (int) (value & 0xFF);

	        DateUtil dt = new DateUtil();
	        if (!dt.isValidTime(hours, minutes, seconds))
	            return "Invalid time";

	        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	    }

	    @Nullable
	    public String getMaxApertureAtFocalLengthDescription()
	    {
	        // Aperture F-Stop = 2^(value/16-0.5)
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_TIME);
	        if (value == null)
	            return null;
	        double fStop = Math.pow((value/16d) - 0.5, 2);
	        return getFStopDescription(fStop);
	    }

	    @Nullable
	    public String getFileNumberMemoryDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_FILE_NUMBER_MEMORY, "Off", "On");
	    }

	    @Nullable
	    public String getLastFileNumberDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_LAST_FILE_NUMBER);
	        return value == null
	            ? null
	            : value == 0
	                ? "File Number Memory Off"
	                : Long.toString(value);
	    }

	    @Nullable
	    public String getWhiteBalanceRedDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_WHITE_BALANCE_RED);
	        DecimalFormat format = new DecimalFormat("0.##");
	        return value == null ? null : format.format(value/256d);
	    }

	    @Nullable
	    public String getWhiteBalanceGreenDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_WHITE_BALANCE_GREEN);
	        DecimalFormat format = new DecimalFormat("0.##");
	        return value == null ? null : format.format(value/256d);
	    }

	    @Nullable
	    public String getWhiteBalanceBlueDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_WHITE_BALANCE_BLUE);
	        DecimalFormat format = new DecimalFormat("0.##");
	        return value == null ? null : format.format(value / 256d);
	    }

	    @Nullable
	    public String getSaturationDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_SATURATION);
	        return value == null ? null : Long.toString(value-3);
	    }

	    @Nullable
	    public String getContrastCameraSettingDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_CONTRAST);
	        return value == null ? null : Long.toString(value-3);
	    }

	    @Nullable
	    public String getSharpnessCameraSettingDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_SHARPNESS, "Hard", "Normal", "Soft");
	    }

	    @Nullable
	    public String getSubjectProgramDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_SUBJECT_PROGRAM, "None", "Portrait", "Text", "Night Portrait", "Sunset", "Sports Action");
	    }

	    @Nullable
	    public String getFlashCompensationDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_FLASH_COMPENSATION);
	        DecimalFormat format = new DecimalFormat("0.##");
	        return value == null ? null : format.format((value-6)/3d) + " EV";
	    }

	    @Nullable
	    public String getIsoSettingDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_ISO_SETTING, "100", "200", "400", "800", "Auto", "64");
	    }

	    @Nullable
	    public String getCameraModelDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_CAMERA_MODEL,
	            "DiMAGE 7",
	            "DiMAGE 5",
	            "DiMAGE S304",
	            "DiMAGE S404",
	            "DiMAGE 7i",
	            "DiMAGE 7Hi",
	            "DiMAGE A1",
	            "DiMAGE S414");
	    }

	    @Nullable
	    public String getIntervalModeDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_INTERVAL_MODE, "Still Image", "Time Lapse Movie");
	    }

	    @Nullable
	    public String getFolderNameDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_FOLDER_NAME, "Standard Form", "Data Form");
	    }

	    @Nullable
	    public String getColorModeCameraSettingDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_COLOR_MODE, "Natural Color", "Black & White", "Vivid Color", "Solarization", "AdobeRGB");
	    }

	    @Nullable
	    public String getColorFilterDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_COLOR_FILTER);
	        return value == null ? null : Long.toString(value-3);
	    }

	    @Nullable
	    public String getBlackAndWhiteFilterDescription()
	    {
	        return super.getDescription(OlympusMakernoteDirectory.CameraSettings.TAG_BLACK_AND_WHITE_FILTER);
	    }

	    @Nullable
	    public String getInternalFlashDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_INTERNAL_FLASH, "Did Not Fire", "Fired");
	    }

	    @Nullable
	    public String getApexBrightnessDescription()
	    {
	        Long value = _directory.getLongObject(OlympusMakernoteDirectory.CameraSettings.TAG_APEX_BRIGHTNESS_VALUE);
	        DecimalFormat format = new DecimalFormat("0.##");
	        return value == null ? null : format.format((value/8d)-6);
	    }

	    @Nullable
	    public String getSpotFocusPointXCoordinateDescription()
	    {
	        return super.getDescription(OlympusMakernoteDirectory.CameraSettings.TAG_SPOT_FOCUS_POINT_X_COORDINATE);
	    }

	    @Nullable
	    public String getSpotFocusPointYCoordinateDescription()
	    {
	        return super.getDescription(OlympusMakernoteDirectory.CameraSettings.TAG_SPOT_FOCUS_POINT_Y_COORDINATE);
	    }

	    @Nullable
	    public String getWideFocusZoneDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_WIDE_FOCUS_ZONE,
	            "No Zone or AF Failed",
	            "Center Zone (Horizontal Orientation)",
	            "Center Zone (Vertical Orientation)",
	            "Left Zone",
	            "Right Zone"
	        );
	    }

	    @Nullable
	    public String getFocusModeCameraSettingDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_FOCUS_MODE, "Auto Focus", "Manual Focus");
	    }

	    @Nullable
	    public String getFocusAreaDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_FOCUS_AREA, "Wide Focus (Normal)", "Spot Focus");
	    }

	    @Nullable
	    public String getDecSwitchPositionDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.CameraSettings.TAG_DEC_SWITCH_POSITION, "Exposure", "Contrast", "Saturation", "Filter");
	    }

	    @Nullable
	    public String getMakernoteVersionDescription()
	    {
	        return getVersionBytesDescription(OlympusMakernoteDirectory.TAG_MAKERNOTE_VERSION, 2);
	    }

	    @Nullable
	    public String getImageQuality2Description()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_IMAGE_QUALITY_2,
	            "Raw",
	            "Super Fine",
	            "Fine",
	            "Standard",
	            "Extra Fine");
	    }

	    @Nullable
	    public String getImageQuality1Description()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_IMAGE_QUALITY_1,
	            "Raw",
	            "Super Fine",
	            "Fine",
	            "Standard",
	            "Extra Fine");
	    }

	    @Nullable
	    public String getColorModeDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_COLOUR_MODE,
	            "Natural Colour",
	            "Black & White",
	            "Vivid Colour",
	            "Solarization",
	            "AdobeRGB");
	    }

	    @Nullable
	    public String getSharpnessDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_SHARPNESS, "Normal", "Hard", "Soft");
	    }

	    @Nullable
	    public String getColorMatrixDescription()
	    {
	        int[] obj = _directory.getIntArray(OlympusMakernoteDirectory.TAG_COLOUR_MATRIX);
	        if (obj == null)
	            return null;

	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < obj.length; i++) {
	            sb.append((short)obj[i]);
	            if (i < obj.length - 1)
	                sb.append(" ");
	        }
	        return sb.length() == 0 ? null : sb.toString();
	    }

	    @Nullable
	    public String getWbModeDescription()
	    {
	        int[] obj = _directory.getIntArray(OlympusMakernoteDirectory.TAG_WB_MODE);
	        if (obj == null)
	            return null;

	        String val = String.format("%d %d", obj[0], obj[1]);

	        if(val.equals("1 0"))
	            return "Auto";
	        else if(val.equals("1 2"))
	            return "Auto (2)";
	        else if(val.equals("1 4"))
	            return "Auto (4)";
	        else if(val.equals("2 2"))
	            return "3000 Kelvin";
	        else if(val.equals("2 3"))
	            return "3700 Kelvin";
	        else if(val.equals("2 4"))
	            return "4000 Kelvin";
	        else if(val.equals("2 5"))
	            return "4500 Kelvin";
	        else if(val.equals("2 6"))
	            return "5500 Kelvin";
	        else if(val.equals("2 7"))
	            return "6500 Kelvin";
	        else if(val.equals("2 8"))
	            return "7500 Kelvin";
	        else if(val.equals("3 0"))
	            return "One-touch";
	        else
	            return "Unknown " + val;
	    }

	    @Nullable
	    public String getRedBalanceDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusMakernoteDirectory.TAG_RED_BALANCE);
	        if (values == null)
	            return null;

	        short value = (short)values[0];

	        return String.valueOf((double)value/256d);
	    }

	    @Nullable
	    public String getBlueBalanceDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusMakernoteDirectory.TAG_BLUE_BALANCE);
	        if (values == null)
	            return null;

	        short value = (short)values[0];

	        return String.valueOf((double)value/256d);
	    }

	    @Nullable
	    public String getContrastDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_CONTRAST, "High", "Normal", "Low");
	    }

	    @Nullable
	    public String getPreviewImageValidDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_PREVIEW_IMAGE_VALID, "No", "Yes");
	    }

	    @Nullable
	    public String getFocusModeDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_FOCUS_MODE, "Auto", "Manual");
	    }

	    @Nullable
	    public String getFocusRangeDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_FOCUS_RANGE, "Normal", "Macro");
	    }

	    @Nullable
	    public String getFlashModeDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_FLASH_MODE, null, null, "On", "Off");
	    }

	    @Nullable
	    public String getDigitalZoomDescription()
	    {
	        Rational value = _directory.getRational(OlympusMakernoteDirectory.TAG_DIGITAL_ZOOM);
	        if (value == null)
	            return null;
	        return value.toSimpleString(false);
	    }

	    @Nullable
	    public String getFocalPlaneDiagonalDescription()
	    {
	        Rational value = _directory.getRational(OlympusMakernoteDirectory.TAG_FOCAL_PLANE_DIAGONAL);
	        if (value == null)
	            return null;

	        DecimalFormat format = new DecimalFormat("0.###");
	        return format.format(value.doubleValue()) + " mm";
	    }

	    @Nullable
	    public String getCameraTypeDescription()
	    {
	        String cameratype = _directory.getString(OlympusMakernoteDirectory.TAG_CAMERA_TYPE);
	        if(cameratype == null)
	            return null;
	        
	        OlympusMakernoteDirectory om = new OlympusMakernoteDirectory();

	        if(om.OlympusCameraTypes.containsKey(cameratype))
	            return om.OlympusCameraTypes.get(cameratype);

	        return cameratype;
	    }

	    @Nullable
	    public String getCameraIdDescription()
	    {
	        byte[] bytes = _directory.getByteArray(OlympusMakernoteDirectory.TAG_CAMERA_ID);
	        if (bytes == null)
	            return null;
	        return new String(bytes);
	    }

	    @Nullable
	    public String getOneTouchWbDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_ONE_TOUCH_WB, "Off", "On", "On (Preset)");
	    }

	    @Nullable
	    public String getShutterSpeedDescription()
	    {
	        return super.getShutterSpeedDescription(OlympusMakernoteDirectory.TAG_SHUTTER_SPEED_VALUE);
	    }

	    @Nullable
	    public String getIsoValueDescription()
	    {
	        Rational value = _directory.getRational(OlympusMakernoteDirectory.TAG_ISO_VALUE);
	        if (value == null)
	            return null;

	        return String.valueOf(Math.round(Math.pow(2, value.doubleValue() - 5) * 100));
	    }

	    @Nullable
	    public String getApertureValueDescription()
	    {
	        Double aperture = _directory.getDoubleObject(OlympusMakernoteDirectory.TAG_APERTURE_VALUE);
	        if (aperture == null)
	            return null;
	        double fStop = PhotographicConversions.apertureToFStop(aperture);
	        return getFStopDescription(fStop);
	    }

	    @Nullable
	    public String getMacroModeDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_MACRO_MODE, "Normal (no macro)", "Macro");
	    }

	    @Nullable
	    public String getBWModeDescription()
	    {
	        return getIndexedDescription(OlympusMakernoteDirectory.TAG_BW_MODE, "Off", "On");
	    }

	    @Nullable
	    public String getJpegQualityDescription()
	    {
	        String cameratype = _directory.getString(OlympusMakernoteDirectory.TAG_CAMERA_TYPE);

	        if(cameratype != null)
	        {
	            Integer value = _directory.getInteger(OlympusMakernoteDirectory.TAG_JPEG_QUALITY);
	            if(value == null)
	                return null;

	            if((cameratype.startsWith("SX") && !cameratype.startsWith("SX151"))
	                || cameratype.startsWith("D4322"))
	            {
	                switch (value)
	                {
	                    case 0:
	                        return "Standard Quality (Low)";
	                    case 1:
	                        return "High Quality (Normal)";
	                    case 2:
	                        return "Super High Quality (Fine)";
	                    case 6:
	                        return "RAW";
	                    default:
	                        return "Unknown (" + value.toString() + ")";
	                }
	            }
	            else
	            {
	                switch (value)
	                {
	                    case 0:
	                        return "Standard Quality (Low)";
	                    case 1:
	                        return "High Quality (Normal)";
	                    case 2:
	                        return "Super High Quality (Fine)";
	                    case 4:
	                        return "RAW";
	                    case 5:
	                        return "Medium-Fine";
	                    case 6:
	                        return "Small-Fine";
	                    case 33:
	                        return "Uncompressed";
	                    default:
	                        return "Unknown (" + value.toString() + ")";
	                }
	            }
	        }
	        else
	            return getIndexedDescription(OlympusMakernoteDirectory.TAG_JPEG_QUALITY,
	            1,
	            "Standard Quality",
	            "High Quality",
	            "Super High Quality");
	    }

	    @Nullable
	    public String getSpecialModeDescription()
	    {
	        long[] values = (long[])_directory.getObject(OlympusMakernoteDirectory.TAG_SPECIAL_MODE);
	        if (values==null)
	            return null;
	        if (values.length < 1)
	            return "";
	        StringBuilder desc = new StringBuilder();

	        switch ((int)values[0]) {
	            case 0:
	                desc.append("Normal picture taking mode");
	                break;
	            case 1:
	                desc.append("Unknown picture taking mode");
	                break;
	            case 2:
	                desc.append("Fast picture taking mode");
	                break;
	            case 3:
	                desc.append("Panorama picture taking mode");
	                break;
	            default:
	                desc.append("Unknown picture taking mode");
	                break;
	        }

	        if (values.length >= 2) {
	            switch ((int)values[1]) {
	                case 0:
	                    break;
	                case 1:
	                    desc.append(" / 1st in a sequence");
	                    break;
	                case 2:
	                    desc.append(" / 2nd in a sequence");
	                    break;
	                case 3:
	                    desc.append(" / 3rd in a sequence");
	                    break;
	                default:
	                    desc.append(" / ");
	                    desc.append(values[1]);
	                    desc.append("th in a sequence");
	                    break;
	            }
	        }
	        if (values.length >= 3) {
	            switch ((int)values[2]) {
	                case 1:
	                    desc.append(" / Left to right panorama direction");
	                    break;
	                case 2:
	                    desc.append(" / Right to left panorama direction");
	                    break;
	                case 3:
	                    desc.append(" / Bottom to top panorama direction");
	                    break;
	                case 4:
	                    desc.append(" / Top to bottom panorama direction");
	                    break;
	            }
	        }

	        return desc.toString();
	    }
	}

	
	/**
	 * The Olympus makernote is used by many manufacturers (Epson, Konica, Minolta and Agfa...), and as such contains some tags
	 * that appear specific to those manufacturers.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusMakernoteDirectory extends Directory
	{
	    /** Used by Konica / Minolta cameras. */
	    public static final int TAG_MAKERNOTE_VERSION = 0x0000;
	    /** Used by Konica / Minolta cameras. */
	    public static final int TAG_CAMERA_SETTINGS_1 = 0x0001;
	    /** Alternate Camera Settings Tag. Used by Konica / Minolta cameras. */
	    public static final int TAG_CAMERA_SETTINGS_2 = 0x0003;
	    /** Used by Konica / Minolta cameras. */
	    public static final int TAG_COMPRESSED_IMAGE_SIZE = 0x0040;
	    /** Used by Konica / Minolta cameras. */
	    public static final int TAG_MINOLTA_THUMBNAIL_OFFSET_1 = 0x0081;
	    /** Alternate Thumbnail Offset. Used by Konica / Minolta cameras. */
	    public static final int TAG_MINOLTA_THUMBNAIL_OFFSET_2 = 0x0088;
	    /** Length of thumbnail in bytes. Used by Konica / Minolta cameras. */
	    public static final int TAG_MINOLTA_THUMBNAIL_LENGTH = 0x0089;

	    public static final int TAG_THUMBNAIL_IMAGE = 0x0100;

	    /**
	     * Used by Konica / Minolta cameras
	     * 0 = Natural Colour
	     * 1 = Black &amp; White
	     * 2 = Vivid colour
	     * 3 = Solarization
	     * 4 = AdobeRGB
	     */
	    public static final int TAG_COLOUR_MODE = 0x0101;

	    /**
	     * Used by Konica / Minolta cameras.
	     * 0 = Raw
	     * 1 = Super Fine
	     * 2 = Fine
	     * 3 = Standard
	     * 4 = Extra Fine
	     */
	    public static final int TAG_IMAGE_QUALITY_1 = 0x0102;

	    /**
	     * Not 100% sure about this tag.
	     * <p>
	     * Used by Konica / Minolta cameras.
	     * 0 = Raw
	     * 1 = Super Fine
	     * 2 = Fine
	     * 3 = Standard
	     * 4 = Extra Fine
	     */
	    public static final int TAG_IMAGE_QUALITY_2 = 0x0103;

	    public static final int TAG_BODY_FIRMWARE_VERSION = 0x0104;

	    /**
	     * Three values:
	     * Value 1: 0=Normal, 2=Fast, 3=Panorama
	     * Value 2: Sequence Number Value 3:
	     * 1 = Panorama Direction: Left to Right
	     * 2 = Panorama Direction: Right to Left
	     * 3 = Panorama Direction: Bottom to Top
	     * 4 = Panorama Direction: Top to Bottom
	     */
	    public static final int TAG_SPECIAL_MODE = 0x0200;

	    /**
	     * 1 = Standard Quality
	     * 2 = High Quality
	     * 3 = Super High Quality
	     */
	    public static final int TAG_JPEG_QUALITY = 0x0201;

	    /**
	     * 0 = Normal (Not Macro)
	     * 1 = Macro
	     */
	    public static final int TAG_MACRO_MODE = 0x0202;

	    /**
	     * 0 = Off, 1 = On
	     */
	    public static final int TAG_BW_MODE = 0x0203;

	    /** Zoom Factor (0 or 1 = normal) */
	    public static final int TAG_DIGITAL_ZOOM = 0x0204;
	    public static final int TAG_FOCAL_PLANE_DIAGONAL = 0x0205;
	    public static final int TAG_LENS_DISTORTION_PARAMETERS = 0x0206;
	    public static final int TAG_CAMERA_TYPE = 0x0207;
	    public static final int TAG_PICT_INFO = 0x0208;
	    public static final int TAG_CAMERA_ID = 0x0209;

	    /**
	     * Used by Epson cameras
	     * Units = pixels
	     */
	    public static final int TAG_IMAGE_WIDTH = 0x020B;

	    /**
	     * Used by Epson cameras
	     * Units = pixels
	     */
	    public static final int TAG_IMAGE_HEIGHT = 0x020C;

	    /** A string. Used by Epson cameras. */
	    public static final int TAG_ORIGINAL_MANUFACTURER_MODEL = 0x020D;

	    public static final int TAG_PREVIEW_IMAGE = 0x0280;
	    public static final int TAG_PRE_CAPTURE_FRAMES = 0x0300;
	    public static final int TAG_WHITE_BOARD = 0x0301;
	    public static final int TAG_ONE_TOUCH_WB = 0x0302;
	    public static final int TAG_WHITE_BALANCE_BRACKET = 0x0303;
	    public static final int TAG_WHITE_BALANCE_BIAS = 0x0304;
	    public static final int TAG_SCENE_MODE = 0x0403;
	    public static final int TAG_SERIAL_NUMBER_1 = 0x0404;
	    public static final int TAG_FIRMWARE = 0x0405;

	    /**
	     * See the PIM specification here:
	     * http://www.ozhiker.com/electronics/pjmt/jpeg_info/pim.html
	     */
	    public static final int TAG_PRINT_IMAGE_MATCHING_INFO = 0x0E00;

	    public static final int TAG_DATA_DUMP_1 = 0x0F00;
	    public static final int TAG_DATA_DUMP_2 = 0x0F01;

	    public static final int TAG_SHUTTER_SPEED_VALUE = 0x1000;
	    public static final int TAG_ISO_VALUE = 0x1001;
	    public static final int TAG_APERTURE_VALUE = 0x1002;
	    public static final int TAG_BRIGHTNESS_VALUE = 0x1003;
	    public static final int TAG_FLASH_MODE = 0x1004;
	    public static final int TAG_FLASH_DEVICE = 0x1005;
	    public static final int TAG_BRACKET = 0x1006;
	    public static final int TAG_SENSOR_TEMPERATURE = 0x1007;
	    public static final int TAG_LENS_TEMPERATURE = 0x1008;
	    public static final int TAG_LIGHT_CONDITION = 0x1009;
	    public static final int TAG_FOCUS_RANGE = 0x100A;
	    public static final int TAG_FOCUS_MODE = 0x100B;
	    public static final int TAG_FOCUS_DISTANCE = 0x100C;
	    public static final int TAG_ZOOM = 0x100D;
	    public static final int TAG_MACRO_FOCUS = 0x100E;
	    public static final int TAG_SHARPNESS = 0x100F;
	    public static final int TAG_FLASH_CHARGE_LEVEL = 0x1010;
	    public static final int TAG_COLOUR_MATRIX = 0x1011;
	    public static final int TAG_BLACK_LEVEL = 0x1012;
	    public static final int TAG_COLOR_TEMPERATURE_BG = 0x1013;
	    public static final int TAG_COLOR_TEMPERATURE_RG = 0x1014;
	    public static final int TAG_WB_MODE = 0x1015;
//	    public static final int TAG_ = 0x1016;
	    public static final int TAG_RED_BALANCE = 0x1017;
	    public static final int TAG_BLUE_BALANCE = 0x1018;
	    public static final int TAG_COLOR_MATRIX_NUMBER = 0x1019;
	    public static final int TAG_SERIAL_NUMBER_2 = 0x101A;

	    public static final int TAG_EXTERNAL_FLASH_AE1_0 = 0x101B;
	    public static final int TAG_EXTERNAL_FLASH_AE2_0 = 0x101C;
	    public static final int TAG_INTERNAL_FLASH_AE1_0 = 0x101D;
	    public static final int TAG_INTERNAL_FLASH_AE2_0 = 0x101E;
	    public static final int TAG_EXTERNAL_FLASH_AE1 = 0x101F;
	    public static final int TAG_EXTERNAL_FLASH_AE2 = 0x1020;
	    public static final int TAG_INTERNAL_FLASH_AE1 = 0x1021;
	    public static final int TAG_INTERNAL_FLASH_AE2 = 0x1022;

	    public static final int TAG_FLASH_BIAS = 0x1023;
	    public static final int TAG_INTERNAL_FLASH_TABLE = 0x1024;
	    public static final int TAG_EXTERNAL_FLASH_G_VALUE = 0x1025;
	    public static final int TAG_EXTERNAL_FLASH_BOUNCE = 0x1026;
	    public static final int TAG_EXTERNAL_FLASH_ZOOM = 0x1027;
	    public static final int TAG_EXTERNAL_FLASH_MODE = 0x1028;
	    public static final int TAG_CONTRAST = 0x1029;
	    public static final int TAG_SHARPNESS_FACTOR = 0x102A;
	    public static final int TAG_COLOUR_CONTROL = 0x102B;
	    public static final int TAG_VALID_BITS = 0x102C;
	    public static final int TAG_CORING_FILTER = 0x102D;
	    public static final int TAG_OLYMPUS_IMAGE_WIDTH = 0x102E;
	    public static final int TAG_OLYMPUS_IMAGE_HEIGHT = 0x102F;
	    public static final int TAG_SCENE_DETECT = 0x1030;
	    public static final int TAG_SCENE_AREA = 0x1031;
//	    public static final int TAG_ = 0x1032;
	    public static final int TAG_SCENE_DETECT_DATA = 0x1033;
	    public static final int TAG_COMPRESSION_RATIO = 0x1034;
	    public static final int TAG_PREVIEW_IMAGE_VALID = 0x1035;
	    public static final int TAG_PREVIEW_IMAGE_START = 0x1036;
	    public static final int TAG_PREVIEW_IMAGE_LENGTH = 0x1037;
	    public static final int TAG_AF_RESULT = 0x1038;
	    public static final int TAG_CCD_SCAN_MODE = 0x1039;
	    public static final int TAG_NOISE_REDUCTION = 0x103A;
	    public static final int TAG_INFINITY_LENS_STEP = 0x103B;
	    public static final int TAG_NEAR_LENS_STEP = 0x103C;
	    public static final int TAG_LIGHT_VALUE_CENTER = 0x103D;
	    public static final int TAG_LIGHT_VALUE_PERIPHERY = 0x103E;
	    public static final int TAG_FIELD_COUNT = 0x103F;
	    public static final int TAG_EQUIPMENT = 0x2010;
	    public static final int TAG_CAMERA_SETTINGS = 0x2020;
	    public static final int TAG_RAW_DEVELOPMENT = 0x2030;
	    public static final int TAG_RAW_DEVELOPMENT_2 = 0x2031;
	    public static final int TAG_IMAGE_PROCESSING = 0x2040;
	    public static final int TAG_FOCUS_INFO = 0x2050;
	    public static final int TAG_RAW_INFO = 0x3000;
	    public static final int TAG_MAIN_INFO = 0x4000;

	    public final class CameraSettings
	    {
	        // These 'sub'-tag values have been created for consistency -- they don't exist within the Makernote IFD
	        private static final int OFFSET = 0xF000;

	        public static final int TAG_EXPOSURE_MODE = OFFSET + 2;
	        public static final int TAG_FLASH_MODE = OFFSET + 3;
	        public static final int TAG_WHITE_BALANCE = OFFSET + 4;
	        public static final int TAG_IMAGE_SIZE = OFFSET + 5;
	        public static final int TAG_IMAGE_QUALITY = OFFSET + 6;
	        public static final int TAG_SHOOTING_MODE = OFFSET + 7;
	        public static final int TAG_METERING_MODE = OFFSET + 8;
	        public static final int TAG_APEX_FILM_SPEED_VALUE = OFFSET + 9;
	        public static final int TAG_APEX_SHUTTER_SPEED_TIME_VALUE = OFFSET + 10;
	        public static final int TAG_APEX_APERTURE_VALUE = OFFSET + 11;
	        public static final int TAG_MACRO_MODE = OFFSET + 12;
	        public static final int TAG_DIGITAL_ZOOM = OFFSET + 13;
	        public static final int TAG_EXPOSURE_COMPENSATION = OFFSET + 14;
	        public static final int TAG_BRACKET_STEP = OFFSET + 15;
	        // 16 missing
	        public static final int TAG_INTERVAL_LENGTH = OFFSET + 17;
	        public static final int TAG_INTERVAL_NUMBER = OFFSET + 18;
	        public static final int TAG_FOCAL_LENGTH = OFFSET + 19;
	        public static final int TAG_FOCUS_DISTANCE = OFFSET + 20;
	        public static final int TAG_FLASH_FIRED = OFFSET + 21;
	        public static final int TAG_DATE = OFFSET + 22;
	        public static final int TAG_TIME = OFFSET + 23;
	        public static final int TAG_MAX_APERTURE_AT_FOCAL_LENGTH = OFFSET + 24;
	        // 25, 26 missing
	        public static final int TAG_FILE_NUMBER_MEMORY = OFFSET + 27;
	        public static final int TAG_LAST_FILE_NUMBER = OFFSET + 28;
	        public static final int TAG_WHITE_BALANCE_RED = OFFSET + 29;
	        public static final int TAG_WHITE_BALANCE_GREEN = OFFSET + 30;
	        public static final int TAG_WHITE_BALANCE_BLUE = OFFSET + 31;
	        public static final int TAG_SATURATION = OFFSET + 32;
	        public static final int TAG_CONTRAST = OFFSET + 33;
	        public static final int TAG_SHARPNESS = OFFSET + 34;
	        public static final int TAG_SUBJECT_PROGRAM = OFFSET + 35;
	        public static final int TAG_FLASH_COMPENSATION = OFFSET + 36;
	        public static final int TAG_ISO_SETTING = OFFSET + 37;
	        public static final int TAG_CAMERA_MODEL = OFFSET + 38;
	        public static final int TAG_INTERVAL_MODE = OFFSET + 39;
	        public static final int TAG_FOLDER_NAME = OFFSET + 40;
	        public static final int TAG_COLOR_MODE = OFFSET + 41;
	        public static final int TAG_COLOR_FILTER = OFFSET + 42;
	        public static final int TAG_BLACK_AND_WHITE_FILTER = OFFSET + 43;
	        public static final int TAG_INTERNAL_FLASH = OFFSET + 44;
	        public static final int TAG_APEX_BRIGHTNESS_VALUE = OFFSET + 45;
	        public static final int TAG_SPOT_FOCUS_POINT_X_COORDINATE = OFFSET + 46;
	        public static final int TAG_SPOT_FOCUS_POINT_Y_COORDINATE = OFFSET + 47;
	        public static final int TAG_WIDE_FOCUS_ZONE = OFFSET + 48;
	        public static final int TAG_FOCUS_MODE = OFFSET + 49;
	        public static final int TAG_FOCUS_AREA = OFFSET + 50;
	        public static final int TAG_DEC_SWITCH_POSITION = OFFSET + 51;
	    }

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TAG_MAKERNOTE_VERSION, "Makernote Version");
	        _tagNameMap.put(TAG_CAMERA_SETTINGS_1, "Camera Settings");
	        _tagNameMap.put(TAG_CAMERA_SETTINGS_2, "Camera Settings");
	        _tagNameMap.put(TAG_COMPRESSED_IMAGE_SIZE, "Compressed Image Size");
	        _tagNameMap.put(TAG_MINOLTA_THUMBNAIL_OFFSET_1, "Thumbnail Offset");
	        _tagNameMap.put(TAG_MINOLTA_THUMBNAIL_OFFSET_2, "Thumbnail Offset");
	        _tagNameMap.put(TAG_MINOLTA_THUMBNAIL_LENGTH, "Thumbnail Length");
	        _tagNameMap.put(TAG_THUMBNAIL_IMAGE, "Thumbnail Image");
	        _tagNameMap.put(TAG_COLOUR_MODE, "Colour Mode");
	        _tagNameMap.put(TAG_IMAGE_QUALITY_1, "Image Quality");
	        _tagNameMap.put(TAG_IMAGE_QUALITY_2, "Image Quality");
	        _tagNameMap.put(TAG_BODY_FIRMWARE_VERSION, "Body Firmware Version");
	        _tagNameMap.put(TAG_SPECIAL_MODE, "Special Mode");
	        _tagNameMap.put(TAG_JPEG_QUALITY, "JPEG Quality");
	        _tagNameMap.put(TAG_MACRO_MODE, "Macro");
	        _tagNameMap.put(TAG_BW_MODE, "BW Mode");
	        _tagNameMap.put(TAG_DIGITAL_ZOOM, "Digital Zoom");
	        _tagNameMap.put(TAG_FOCAL_PLANE_DIAGONAL, "Focal Plane Diagonal");
	        _tagNameMap.put(TAG_LENS_DISTORTION_PARAMETERS, "Lens Distortion Parameters");
	        _tagNameMap.put(TAG_CAMERA_TYPE, "Camera Type");
	        _tagNameMap.put(TAG_PICT_INFO, "Pict Info");
	        _tagNameMap.put(TAG_CAMERA_ID, "Camera Id");
	        _tagNameMap.put(TAG_IMAGE_WIDTH, "Image Width");
	        _tagNameMap.put(TAG_IMAGE_HEIGHT, "Image Height");
	        _tagNameMap.put(TAG_ORIGINAL_MANUFACTURER_MODEL, "Original Manufacturer Model");
	        _tagNameMap.put(TAG_PREVIEW_IMAGE, "Preview Image");
	        _tagNameMap.put(TAG_PRE_CAPTURE_FRAMES, "Pre Capture Frames");
	        _tagNameMap.put(TAG_WHITE_BOARD, "White Board");
	        _tagNameMap.put(TAG_ONE_TOUCH_WB, "One Touch WB");
	        _tagNameMap.put(TAG_WHITE_BALANCE_BRACKET, "White Balance Bracket");
	        _tagNameMap.put(TAG_WHITE_BALANCE_BIAS, "White Balance Bias");
	        _tagNameMap.put(TAG_SCENE_MODE, "Scene Mode");
	        _tagNameMap.put(TAG_SERIAL_NUMBER_1, "Serial Number");
	        _tagNameMap.put(TAG_FIRMWARE, "Firmware");
	        _tagNameMap.put(TAG_PRINT_IMAGE_MATCHING_INFO, "Print Image Matching (PIM) Info");
	        _tagNameMap.put(TAG_DATA_DUMP_1, "Data Dump");
	        _tagNameMap.put(TAG_DATA_DUMP_2, "Data Dump 2");
	        _tagNameMap.put(TAG_SHUTTER_SPEED_VALUE, "Shutter Speed Value");
	        _tagNameMap.put(TAG_ISO_VALUE, "ISO Value");
	        _tagNameMap.put(TAG_APERTURE_VALUE, "Aperture Value");
	        _tagNameMap.put(TAG_BRIGHTNESS_VALUE, "Brightness Value");
	        _tagNameMap.put(TAG_FLASH_MODE, "Flash Mode");
	        _tagNameMap.put(TAG_FLASH_DEVICE, "Flash Device");
	        _tagNameMap.put(TAG_BRACKET, "Bracket");
	        _tagNameMap.put(TAG_SENSOR_TEMPERATURE, "Sensor Temperature");
	        _tagNameMap.put(TAG_LENS_TEMPERATURE, "Lens Temperature");
	        _tagNameMap.put(TAG_LIGHT_CONDITION, "Light Condition");
	        _tagNameMap.put(TAG_FOCUS_RANGE, "Focus Range");
	        _tagNameMap.put(TAG_FOCUS_MODE, "Focus Mode");
	        _tagNameMap.put(TAG_FOCUS_DISTANCE, "Focus Distance");
	        _tagNameMap.put(TAG_ZOOM, "Zoom");
	        _tagNameMap.put(TAG_MACRO_FOCUS, "Macro Focus");
	        _tagNameMap.put(TAG_SHARPNESS, "Sharpness");
	        _tagNameMap.put(TAG_FLASH_CHARGE_LEVEL, "Flash Charge Level");
	        _tagNameMap.put(TAG_COLOUR_MATRIX, "Colour Matrix");
	        _tagNameMap.put(TAG_BLACK_LEVEL, "Black Level");
	        _tagNameMap.put(TAG_COLOR_TEMPERATURE_BG, "Color Temperature BG");
	        _tagNameMap.put(TAG_COLOR_TEMPERATURE_RG, "Color Temperature RG");
	        _tagNameMap.put(TAG_WB_MODE, "White Balance Mode");
	        _tagNameMap.put(TAG_RED_BALANCE, "Red Balance");
	        _tagNameMap.put(TAG_BLUE_BALANCE, "Blue Balance");
	        _tagNameMap.put(TAG_COLOR_MATRIX_NUMBER, "Color Matrix Number");
	        _tagNameMap.put(TAG_SERIAL_NUMBER_2, "Serial Number");
	        _tagNameMap.put(TAG_EXTERNAL_FLASH_AE1_0, "External Flash AE1 0");
	        _tagNameMap.put(TAG_EXTERNAL_FLASH_AE2_0, "External Flash AE2 0");
	        _tagNameMap.put(TAG_INTERNAL_FLASH_AE1_0, "Internal Flash AE1 0");
	        _tagNameMap.put(TAG_INTERNAL_FLASH_AE2_0, "Internal Flash AE2 0");
	        _tagNameMap.put(TAG_EXTERNAL_FLASH_AE1, "External Flash AE1");
	        _tagNameMap.put(TAG_EXTERNAL_FLASH_AE2, "External Flash AE2");
	        _tagNameMap.put(TAG_INTERNAL_FLASH_AE1, "Internal Flash AE1");
	        _tagNameMap.put(TAG_INTERNAL_FLASH_AE2, "Internal Flash AE2");
	        _tagNameMap.put(TAG_FLASH_BIAS, "Flash Bias");
	        _tagNameMap.put(TAG_INTERNAL_FLASH_TABLE, "Internal Flash Table");
	        _tagNameMap.put(TAG_EXTERNAL_FLASH_G_VALUE, "External Flash G Value");
	        _tagNameMap.put(TAG_EXTERNAL_FLASH_BOUNCE, "External Flash Bounce");
	        _tagNameMap.put(TAG_EXTERNAL_FLASH_ZOOM, "External Flash Zoom");
	        _tagNameMap.put(TAG_EXTERNAL_FLASH_MODE, "External Flash Mode");
	        _tagNameMap.put(TAG_CONTRAST, "Contrast");
	        _tagNameMap.put(TAG_SHARPNESS_FACTOR, "Sharpness Factor");
	        _tagNameMap.put(TAG_COLOUR_CONTROL, "Colour Control");
	        _tagNameMap.put(TAG_VALID_BITS, "Valid Bits");
	        _tagNameMap.put(TAG_CORING_FILTER, "Coring Filter");
	        _tagNameMap.put(TAG_OLYMPUS_IMAGE_WIDTH, "Olympus Image Width");
	        _tagNameMap.put(TAG_OLYMPUS_IMAGE_HEIGHT, "Olympus Image Height");
	        _tagNameMap.put(TAG_SCENE_DETECT, "Scene Detect");
	        _tagNameMap.put(TAG_SCENE_AREA, "Scene Area");
	        _tagNameMap.put(TAG_SCENE_DETECT_DATA, "Scene Detect Data");
	        _tagNameMap.put(TAG_COMPRESSION_RATIO, "Compression Ratio");
	        _tagNameMap.put(TAG_PREVIEW_IMAGE_VALID, "Preview Image Valid");
	        _tagNameMap.put(TAG_PREVIEW_IMAGE_START, "Preview Image Start");
	        _tagNameMap.put(TAG_PREVIEW_IMAGE_LENGTH, "Preview Image Length");
	        _tagNameMap.put(TAG_AF_RESULT, "AF Result");
	        _tagNameMap.put(TAG_CCD_SCAN_MODE, "CCD Scan Mode");
	        _tagNameMap.put(TAG_NOISE_REDUCTION, "Noise Reduction");
	        _tagNameMap.put(TAG_INFINITY_LENS_STEP, "Infinity Lens Step");
	        _tagNameMap.put(TAG_NEAR_LENS_STEP, "Near Lens Step");
	        _tagNameMap.put(TAG_LIGHT_VALUE_CENTER, "Light Value Center");
	        _tagNameMap.put(TAG_LIGHT_VALUE_PERIPHERY, "Light Value Periphery");
	        _tagNameMap.put(TAG_FIELD_COUNT, "Field Count");
	        _tagNameMap.put(TAG_EQUIPMENT, "Equipment");
	        _tagNameMap.put(TAG_CAMERA_SETTINGS, "Camera Settings");
	        _tagNameMap.put(TAG_RAW_DEVELOPMENT, "Raw Development");
	        _tagNameMap.put(TAG_RAW_DEVELOPMENT_2, "Raw Development 2");
	        _tagNameMap.put(TAG_IMAGE_PROCESSING, "Image Processing");
	        _tagNameMap.put(TAG_FOCUS_INFO, "Focus Info");
	        _tagNameMap.put(TAG_RAW_INFO, "Raw Info");
	        _tagNameMap.put(TAG_MAIN_INFO, "Main Info");

	        _tagNameMap.put(CameraSettings.TAG_EXPOSURE_MODE, "Exposure Mode");
	        _tagNameMap.put(CameraSettings.TAG_FLASH_MODE, "Flash Mode");
	        _tagNameMap.put(CameraSettings.TAG_WHITE_BALANCE, "White Balance");
	        _tagNameMap.put(CameraSettings.TAG_IMAGE_SIZE, "Image Size");
	        _tagNameMap.put(CameraSettings.TAG_IMAGE_QUALITY, "Image Quality");
	        _tagNameMap.put(CameraSettings.TAG_SHOOTING_MODE, "Shooting Mode");
	        _tagNameMap.put(CameraSettings.TAG_METERING_MODE, "Metering Mode");
	        _tagNameMap.put(CameraSettings.TAG_APEX_FILM_SPEED_VALUE, "Apex Film Speed Value");
	        _tagNameMap.put(CameraSettings.TAG_APEX_SHUTTER_SPEED_TIME_VALUE, "Apex Shutter Speed Time Value");
	        _tagNameMap.put(CameraSettings.TAG_APEX_APERTURE_VALUE, "Apex Aperture Value");
	        _tagNameMap.put(CameraSettings.TAG_MACRO_MODE, "Macro Mode");
	        _tagNameMap.put(CameraSettings.TAG_DIGITAL_ZOOM, "Digital Zoom");
	        _tagNameMap.put(CameraSettings.TAG_EXPOSURE_COMPENSATION, "Exposure Compensation");
	        _tagNameMap.put(CameraSettings.TAG_BRACKET_STEP, "Bracket Step");

	        _tagNameMap.put(CameraSettings.TAG_INTERVAL_LENGTH, "Interval Length");
	        _tagNameMap.put(CameraSettings.TAG_INTERVAL_NUMBER, "Interval Number");
	        _tagNameMap.put(CameraSettings.TAG_FOCAL_LENGTH, "Focal Length");
	        _tagNameMap.put(CameraSettings.TAG_FOCUS_DISTANCE, "Focus Distance");
	        _tagNameMap.put(CameraSettings.TAG_FLASH_FIRED, "Flash Fired");
	        _tagNameMap.put(CameraSettings.TAG_DATE, "Date");
	        _tagNameMap.put(CameraSettings.TAG_TIME, "Time");
	        _tagNameMap.put(CameraSettings.TAG_MAX_APERTURE_AT_FOCAL_LENGTH, "Max Aperture at Focal Length");

	        _tagNameMap.put(CameraSettings.TAG_FILE_NUMBER_MEMORY, "File Number Memory");
	        _tagNameMap.put(CameraSettings.TAG_LAST_FILE_NUMBER, "Last File Number");
	        _tagNameMap.put(CameraSettings.TAG_WHITE_BALANCE_RED, "White Balance Red");
	        _tagNameMap.put(CameraSettings.TAG_WHITE_BALANCE_GREEN, "White Balance Green");
	        _tagNameMap.put(CameraSettings.TAG_WHITE_BALANCE_BLUE, "White Balance Blue");
	        _tagNameMap.put(CameraSettings.TAG_SATURATION, "Saturation");
	        _tagNameMap.put(CameraSettings.TAG_CONTRAST, "Contrast");
	        _tagNameMap.put(CameraSettings.TAG_SHARPNESS, "Sharpness");
	        _tagNameMap.put(CameraSettings.TAG_SUBJECT_PROGRAM, "Subject Program");
	        _tagNameMap.put(CameraSettings.TAG_FLASH_COMPENSATION, "Flash Compensation");
	        _tagNameMap.put(CameraSettings.TAG_ISO_SETTING, "ISO Setting");
	        _tagNameMap.put(CameraSettings.TAG_CAMERA_MODEL, "Camera Model");
	        _tagNameMap.put(CameraSettings.TAG_INTERVAL_MODE, "Interval Mode");
	        _tagNameMap.put(CameraSettings.TAG_FOLDER_NAME, "Folder Name");
	        _tagNameMap.put(CameraSettings.TAG_COLOR_MODE, "Color Mode");
	        _tagNameMap.put(CameraSettings.TAG_COLOR_FILTER, "Color Filter");
	        _tagNameMap.put(CameraSettings.TAG_BLACK_AND_WHITE_FILTER, "Black and White Filter");
	        _tagNameMap.put(CameraSettings.TAG_INTERNAL_FLASH, "Internal Flash");
	        _tagNameMap.put(CameraSettings.TAG_APEX_BRIGHTNESS_VALUE, "Apex Brightness Value");
	        _tagNameMap.put(CameraSettings.TAG_SPOT_FOCUS_POINT_X_COORDINATE, "Spot Focus Point X Coordinate");
	        _tagNameMap.put(CameraSettings.TAG_SPOT_FOCUS_POINT_Y_COORDINATE, "Spot Focus Point Y Coordinate");
	        _tagNameMap.put(CameraSettings.TAG_WIDE_FOCUS_ZONE, "Wide Focus Zone");
	        _tagNameMap.put(CameraSettings.TAG_FOCUS_MODE, "Focus Mode");
	        _tagNameMap.put(CameraSettings.TAG_FOCUS_AREA, "Focus Area");
	        _tagNameMap.put(CameraSettings.TAG_DEC_SWITCH_POSITION, "DEC Switch Position");
	    }

	    public OlympusMakernoteDirectory()
	    {
	        this.setDescriptor(new OlympusMakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Olympus Makernote";
	    }

	    @Override
	    public void setByteArray(int tagType, @NotNull byte[] bytes)
	    {
	        if (tagType == TAG_CAMERA_SETTINGS_1 || tagType == TAG_CAMERA_SETTINGS_2) {
	            processCameraSettings(bytes);
	        } else {
	            super.setByteArray(tagType, bytes);
	        }
	    }

	    private void processCameraSettings(byte[] bytes)
	    {
	        SequentialByteArrayReader reader = new SequentialByteArrayReader(bytes);
	        reader.setMotorolaByteOrder(true);

	        int count = bytes.length / 4;

	        try {
	            for (int i = 0; i < count; i++) {
	                int value = reader.getInt32();
	                setInt(CameraSettings.OFFSET + i, value);
	            }
	        } catch (IOException e) {
	            // Should never happen, given that we check the length of the bytes beforehand.
	        }
	    }

	    public boolean isIntervalMode()
	    {
	        Long value = getLongObject(CameraSettings.TAG_SHOOTING_MODE);
	        return value != null && value == 5;
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }

	    /**
	     * These values are currently decoded only for Olympus models.  Models with
	     * Olympus-style maker notes from other brands such as Acer, BenQ, Hitachi, HP,
	     * Premier, Konica-Minolta, Maginon, Ricoh, Rollei, SeaLife, Sony, Supra,
	     * Vivitar are not listed.
	     *
	     *
	     * Converted from Exiftool version 10.33 created by Phil Harvey
	     * http://www.sno.phy.queensu.ca/~phil/exiftool/
	     * lib\Image\ExifTool\Olympus.pm
	     */
	    final HashMap<String, String> OlympusCameraTypes = new HashMap<String, String>();

	    {
	        OlympusCameraTypes.put("D4028", "X-2,C-50Z");
	        OlympusCameraTypes.put("D4029", "E-20,E-20N,E-20P");
	        OlympusCameraTypes.put("D4034", "C720UZ");
	        OlympusCameraTypes.put("D4040", "E-1");
	        OlympusCameraTypes.put("D4041", "E-300");
	        OlympusCameraTypes.put("D4083", "C2Z,D520Z,C220Z");
	        OlympusCameraTypes.put("D4106", "u20D,S400D,u400D");
	        OlympusCameraTypes.put("D4120", "X-1");
	        OlympusCameraTypes.put("D4122", "u10D,S300D,u300D");
	        OlympusCameraTypes.put("D4125", "AZ-1");
	        OlympusCameraTypes.put("D4141", "C150,D390");
	        OlympusCameraTypes.put("D4193", "C-5000Z");
	        OlympusCameraTypes.put("D4194", "X-3,C-60Z");
	        OlympusCameraTypes.put("D4199", "u30D,S410D,u410D");
	        OlympusCameraTypes.put("D4205", "X450,D535Z,C370Z");
	        OlympusCameraTypes.put("D4210", "C160,D395");
	        OlympusCameraTypes.put("D4211", "C725UZ");
	        OlympusCameraTypes.put("D4213", "FerrariMODEL2003");
	        OlympusCameraTypes.put("D4216", "u15D");
	        OlympusCameraTypes.put("D4217", "u25D");
	        OlympusCameraTypes.put("D4220", "u-miniD,Stylus V");
	        OlympusCameraTypes.put("D4221", "u40D,S500,uD500");
	        OlympusCameraTypes.put("D4231", "FerrariMODEL2004");
	        OlympusCameraTypes.put("D4240", "X500,D590Z,C470Z");
	        OlympusCameraTypes.put("D4244", "uD800,S800");
	        OlympusCameraTypes.put("D4256", "u720SW,S720SW");
	        OlympusCameraTypes.put("D4261", "X600,D630,FE5500");
	        OlympusCameraTypes.put("D4262", "uD600,S600");
	        OlympusCameraTypes.put("D4301", "u810/S810"); // (yes, "/".  Olympus is not consistent in the notation)
	        OlympusCameraTypes.put("D4302", "u710,S710");
	        OlympusCameraTypes.put("D4303", "u700,S700");
	        OlympusCameraTypes.put("D4304", "FE100,X710");
	        OlympusCameraTypes.put("D4305", "FE110,X705");
	        OlympusCameraTypes.put("D4310", "FE-130,X-720");
	        OlympusCameraTypes.put("D4311", "FE-140,X-725");
	        OlympusCameraTypes.put("D4312", "FE150,X730");
	        OlympusCameraTypes.put("D4313", "FE160,X735");
	        OlympusCameraTypes.put("D4314", "u740,S740");
	        OlympusCameraTypes.put("D4315", "u750,S750");
	        OlympusCameraTypes.put("D4316", "u730/S730");
	        OlympusCameraTypes.put("D4317", "FE115,X715");
	        OlympusCameraTypes.put("D4321", "SP550UZ");
	        OlympusCameraTypes.put("D4322", "SP510UZ");
	        OlympusCameraTypes.put("D4324", "FE170,X760");
	        OlympusCameraTypes.put("D4326", "FE200");
	        OlympusCameraTypes.put("D4327", "FE190/X750"); // (also SX876)
	        OlympusCameraTypes.put("D4328", "u760,S760");
	        OlympusCameraTypes.put("D4330", "FE180/X745"); // (also SX875)
	        OlympusCameraTypes.put("D4331", "u1000/S1000");
	        OlympusCameraTypes.put("D4332", "u770SW,S770SW");
	        OlympusCameraTypes.put("D4333", "FE240/X795");
	        OlympusCameraTypes.put("D4334", "FE210,X775");
	        OlympusCameraTypes.put("D4336", "FE230/X790");
	        OlympusCameraTypes.put("D4337", "FE220,X785");
	        OlympusCameraTypes.put("D4338", "u725SW,S725SW");
	        OlympusCameraTypes.put("D4339", "FE250/X800");
	        OlympusCameraTypes.put("D4341", "u780,S780");
	        OlympusCameraTypes.put("D4343", "u790SW,S790SW");
	        OlympusCameraTypes.put("D4344", "u1020,S1020");
	        OlympusCameraTypes.put("D4346", "FE15,X10");
	        OlympusCameraTypes.put("D4348", "FE280,X820,C520");
	        OlympusCameraTypes.put("D4349", "FE300,X830");
	        OlympusCameraTypes.put("D4350", "u820,S820");
	        OlympusCameraTypes.put("D4351", "u1200,S1200");
	        OlympusCameraTypes.put("D4352", "FE270,X815,C510");
	        OlympusCameraTypes.put("D4353", "u795SW,S795SW");
	        OlympusCameraTypes.put("D4354", "u1030SW,S1030SW");
	        OlympusCameraTypes.put("D4355", "SP560UZ");
	        OlympusCameraTypes.put("D4356", "u1010,S1010");
	        OlympusCameraTypes.put("D4357", "u830,S830");
	        OlympusCameraTypes.put("D4359", "u840,S840");
	        OlympusCameraTypes.put("D4360", "FE350WIDE,X865");
	        OlympusCameraTypes.put("D4361", "u850SW,S850SW");
	        OlympusCameraTypes.put("D4362", "FE340,X855,C560");
	        OlympusCameraTypes.put("D4363", "FE320,X835,C540");
	        OlympusCameraTypes.put("D4364", "SP570UZ");
	        OlympusCameraTypes.put("D4366", "FE330,X845,C550");
	        OlympusCameraTypes.put("D4368", "FE310,X840,C530");
	        OlympusCameraTypes.put("D4370", "u1050SW,S1050SW");
	        OlympusCameraTypes.put("D4371", "u1060,S1060");
	        OlympusCameraTypes.put("D4372", "FE370,X880,C575");
	        OlympusCameraTypes.put("D4374", "SP565UZ");
	        OlympusCameraTypes.put("D4377", "u1040,S1040");
	        OlympusCameraTypes.put("D4378", "FE360,X875,C570");
	        OlympusCameraTypes.put("D4379", "FE20,X15,C25");
	        OlympusCameraTypes.put("D4380", "uT6000,ST6000");
	        OlympusCameraTypes.put("D4381", "uT8000,ST8000");
	        OlympusCameraTypes.put("D4382", "u9000,S9000");
	        OlympusCameraTypes.put("D4384", "SP590UZ");
	        OlympusCameraTypes.put("D4385", "FE3010,X895");
	        OlympusCameraTypes.put("D4386", "FE3000,X890");
	        OlympusCameraTypes.put("D4387", "FE35,X30");
	        OlympusCameraTypes.put("D4388", "u550WP,S550WP");
	        OlympusCameraTypes.put("D4390", "FE5000,X905");
	        OlympusCameraTypes.put("D4391", "u5000");
	        OlympusCameraTypes.put("D4392", "u7000,S7000");
	        OlympusCameraTypes.put("D4396", "FE5010,X915");
	        OlympusCameraTypes.put("D4397", "FE25,X20");
	        OlympusCameraTypes.put("D4398", "FE45,X40");
	        OlympusCameraTypes.put("D4401", "XZ-1");
	        OlympusCameraTypes.put("D4402", "uT6010,ST6010");
	        OlympusCameraTypes.put("D4406", "u7010,S7010 / u7020,S7020");
	        OlympusCameraTypes.put("D4407", "FE4010,X930");
	        OlympusCameraTypes.put("D4408", "X560WP");
	        OlympusCameraTypes.put("D4409", "FE26,X21");
	        OlympusCameraTypes.put("D4410", "FE4000,X920,X925");
	        OlympusCameraTypes.put("D4411", "FE46,X41,X42");
	        OlympusCameraTypes.put("D4412", "FE5020,X935");
	        OlympusCameraTypes.put("D4413", "uTough-3000");
	        OlympusCameraTypes.put("D4414", "StylusTough-6020");
	        OlympusCameraTypes.put("D4415", "StylusTough-8010");
	        OlympusCameraTypes.put("D4417", "u5010,S5010");
	        OlympusCameraTypes.put("D4418", "u7040,S7040");
	        OlympusCameraTypes.put("D4419", "u9010,S9010");
	        OlympusCameraTypes.put("D4423", "FE4040");
	        OlympusCameraTypes.put("D4424", "FE47,X43");
	        OlympusCameraTypes.put("D4426", "FE4030,X950");
	        OlympusCameraTypes.put("D4428", "FE5030,X965,X960");
	        OlympusCameraTypes.put("D4430", "u7030,S7030");
	        OlympusCameraTypes.put("D4432", "SP600UZ");
	        OlympusCameraTypes.put("D4434", "SP800UZ");
	        OlympusCameraTypes.put("D4439", "FE4020,X940");
	        OlympusCameraTypes.put("D4442", "FE5035");
	        OlympusCameraTypes.put("D4448", "FE4050,X970");
	        OlympusCameraTypes.put("D4450", "FE5050,X985");
	        OlympusCameraTypes.put("D4454", "u-7050");
	        OlympusCameraTypes.put("D4464", "T10,X27");
	        OlympusCameraTypes.put("D4470", "FE5040,X980");
	        OlympusCameraTypes.put("D4472", "TG-310");
	        OlympusCameraTypes.put("D4474", "TG-610");
	        OlympusCameraTypes.put("D4476", "TG-810");
	        OlympusCameraTypes.put("D4478", "VG145,VG140,D715");
	        OlympusCameraTypes.put("D4479", "VG130,D710");
	        OlympusCameraTypes.put("D4480", "VG120,D705");
	        OlympusCameraTypes.put("D4482", "VR310,D720");
	        OlympusCameraTypes.put("D4484", "VR320,D725");
	        OlympusCameraTypes.put("D4486", "VR330,D730");
	        OlympusCameraTypes.put("D4488", "VG110,D700");
	        OlympusCameraTypes.put("D4490", "SP-610UZ");
	        OlympusCameraTypes.put("D4492", "SZ-10");
	        OlympusCameraTypes.put("D4494", "SZ-20");
	        OlympusCameraTypes.put("D4496", "SZ-30MR");
	        OlympusCameraTypes.put("D4498", "SP-810UZ");
	        OlympusCameraTypes.put("D4500", "SZ-11");
	        OlympusCameraTypes.put("D4504", "TG-615");
	        OlympusCameraTypes.put("D4508", "TG-620");
	        OlympusCameraTypes.put("D4510", "TG-820");
	        OlympusCameraTypes.put("D4512", "TG-1");
	        OlympusCameraTypes.put("D4516", "SH-21");
	        OlympusCameraTypes.put("D4519", "SZ-14");
	        OlympusCameraTypes.put("D4520", "SZ-31MR");
	        OlympusCameraTypes.put("D4521", "SH-25MR");
	        OlympusCameraTypes.put("D4523", "SP-720UZ");
	        OlympusCameraTypes.put("D4529", "VG170");
	        OlympusCameraTypes.put("D4531", "XZ-2");
	        OlympusCameraTypes.put("D4535", "SP-620UZ");
	        OlympusCameraTypes.put("D4536", "TG-320");
	        OlympusCameraTypes.put("D4537", "VR340,D750");
	        OlympusCameraTypes.put("D4538", "VG160,X990,D745");
	        OlympusCameraTypes.put("D4541", "SZ-12");
	        OlympusCameraTypes.put("D4545", "VH410");
	        OlympusCameraTypes.put("D4546", "XZ-10"); //IB
	        OlympusCameraTypes.put("D4547", "TG-2");
	        OlympusCameraTypes.put("D4548", "TG-830");
	        OlympusCameraTypes.put("D4549", "TG-630");
	        OlympusCameraTypes.put("D4550", "SH-50");
	        OlympusCameraTypes.put("D4553", "SZ-16,DZ-105");
	        OlympusCameraTypes.put("D4562", "SP-820UZ");
	        OlympusCameraTypes.put("D4566", "SZ-15");
	        OlympusCameraTypes.put("D4572", "STYLUS1");
	        OlympusCameraTypes.put("D4574", "TG-3");
	        OlympusCameraTypes.put("D4575", "TG-850");
	        OlympusCameraTypes.put("D4579", "SP-100EE");
	        OlympusCameraTypes.put("D4580", "SH-60");
	        OlympusCameraTypes.put("D4581", "SH-1");
	        OlympusCameraTypes.put("D4582", "TG-835");
	        OlympusCameraTypes.put("D4585", "SH-2 / SH-3");
	        OlympusCameraTypes.put("D4586", "TG-4");
	        OlympusCameraTypes.put("D4587", "TG-860");
	        OlympusCameraTypes.put("D4591", "TG-870");
	        OlympusCameraTypes.put("D4593", "TG-5");
	        OlympusCameraTypes.put("D4809", "C2500L");
	        OlympusCameraTypes.put("D4842", "E-10");
	        OlympusCameraTypes.put("D4856", "C-1");
	        OlympusCameraTypes.put("D4857", "C-1Z,D-150Z");
	        OlympusCameraTypes.put("DCHC", "D500L");
	        OlympusCameraTypes.put("DCHT", "D600L / D620L");
	        OlympusCameraTypes.put("K0055", "AIR-A01");
	        OlympusCameraTypes.put("S0003", "E-330");
	        OlympusCameraTypes.put("S0004", "E-500");
	        OlympusCameraTypes.put("S0009", "E-400");
	        OlympusCameraTypes.put("S0010", "E-510");
	        OlympusCameraTypes.put("S0011", "E-3");
	        OlympusCameraTypes.put("S0013", "E-410");
	        OlympusCameraTypes.put("S0016", "E-420");
	        OlympusCameraTypes.put("S0017", "E-30");
	        OlympusCameraTypes.put("S0018", "E-520");
	        OlympusCameraTypes.put("S0019", "E-P1");
	        OlympusCameraTypes.put("S0023", "E-620");
	        OlympusCameraTypes.put("S0026", "E-P2");
	        OlympusCameraTypes.put("S0027", "E-PL1");
	        OlympusCameraTypes.put("S0029", "E-450");
	        OlympusCameraTypes.put("S0030", "E-600");
	        OlympusCameraTypes.put("S0032", "E-P3");
	        OlympusCameraTypes.put("S0033", "E-5");
	        OlympusCameraTypes.put("S0034", "E-PL2");
	        OlympusCameraTypes.put("S0036", "E-M5");
	        OlympusCameraTypes.put("S0038", "E-PL3");
	        OlympusCameraTypes.put("S0039", "E-PM1");
	        OlympusCameraTypes.put("S0040", "E-PL1s");
	        OlympusCameraTypes.put("S0042", "E-PL5");
	        OlympusCameraTypes.put("S0043", "E-PM2");
	        OlympusCameraTypes.put("S0044", "E-P5");
	        OlympusCameraTypes.put("S0045", "E-PL6");
	        OlympusCameraTypes.put("S0046", "E-PL7"); //IB
	        OlympusCameraTypes.put("S0047", "E-M1");
	        OlympusCameraTypes.put("S0051", "E-M10");
	        OlympusCameraTypes.put("S0052", "E-M5MarkII"); //IB
	        OlympusCameraTypes.put("S0059", "E-M10MarkII");
	        OlympusCameraTypes.put("S0061", "PEN-F"); //forum7005
	        OlympusCameraTypes.put("S0065", "E-PL8");
	        OlympusCameraTypes.put("S0067", "E-M1MarkII");
	        OlympusCameraTypes.put("SR45", "D220");
	        OlympusCameraTypes.put("SR55", "D320L");
	        OlympusCameraTypes.put("SR83", "D340L");
	        OlympusCameraTypes.put("SR85", "C830L,D340R");
	        OlympusCameraTypes.put("SR852", "C860L,D360L");
	        OlympusCameraTypes.put("SR872", "C900Z,D400Z");
	        OlympusCameraTypes.put("SR874", "C960Z,D460Z");
	        OlympusCameraTypes.put("SR951", "C2000Z");
	        OlympusCameraTypes.put("SR952", "C21");
	        OlympusCameraTypes.put("SR953", "C21T.commu");
	        OlympusCameraTypes.put("SR954", "C2020Z");
	        OlympusCameraTypes.put("SR955", "C990Z,D490Z");
	        OlympusCameraTypes.put("SR956", "C211Z");
	        OlympusCameraTypes.put("SR959", "C990ZS,D490Z");
	        OlympusCameraTypes.put("SR95A", "C2100UZ");
	        OlympusCameraTypes.put("SR971", "C100,D370");
	        OlympusCameraTypes.put("SR973", "C2,D230");
	        OlympusCameraTypes.put("SX151", "E100RS");
	        OlympusCameraTypes.put("SX351", "C3000Z / C3030Z");
	        OlympusCameraTypes.put("SX354", "C3040Z");
	        OlympusCameraTypes.put("SX355", "C2040Z");
	        OlympusCameraTypes.put("SX357", "C700UZ");
	        OlympusCameraTypes.put("SX358", "C200Z,D510Z");
	        OlympusCameraTypes.put("SX374", "C3100Z,C3020Z");
	        OlympusCameraTypes.put("SX552", "C4040Z");
	        OlympusCameraTypes.put("SX553", "C40Z,D40Z");
	        OlympusCameraTypes.put("SX556", "C730UZ");
	        OlympusCameraTypes.put("SX558", "C5050Z");
	        OlympusCameraTypes.put("SX571", "C120,D380");
	        OlympusCameraTypes.put("SX574", "C300Z,D550Z");
	        OlympusCameraTypes.put("SX575", "C4100Z,C4000Z");
	        OlympusCameraTypes.put("SX751", "X200,D560Z,C350Z");
	        OlympusCameraTypes.put("SX752", "X300,D565Z,C450Z");
	        OlympusCameraTypes.put("SX753", "C750UZ");
	        OlympusCameraTypes.put("SX754", "C740UZ");
	        OlympusCameraTypes.put("SX755", "C755UZ");
	        OlympusCameraTypes.put("SX756", "C5060WZ");
	        OlympusCameraTypes.put("SX757", "C8080WZ");
	        OlympusCameraTypes.put("SX758", "X350,D575Z,C360Z");
	        OlympusCameraTypes.put("SX759", "X400,D580Z,C460Z");
	        OlympusCameraTypes.put("SX75A", "AZ-2ZOOM");
	        OlympusCameraTypes.put("SX75B", "D595Z,C500Z");
	        OlympusCameraTypes.put("SX75C", "X550,D545Z,C480Z");
	        OlympusCameraTypes.put("SX75D", "IR-300");
	        OlympusCameraTypes.put("SX75F", "C55Z,C5500Z");
	        OlympusCameraTypes.put("SX75G", "C170,D425");
	        OlympusCameraTypes.put("SX75J", "C180,D435");
	        OlympusCameraTypes.put("SX771", "C760UZ");
	        OlympusCameraTypes.put("SX772", "C770UZ");
	        OlympusCameraTypes.put("SX773", "C745UZ");
	        OlympusCameraTypes.put("SX774", "X250,D560Z,C350Z");
	        OlympusCameraTypes.put("SX775", "X100,D540Z,C310Z");
	        OlympusCameraTypes.put("SX776", "C460ZdelSol");
	        OlympusCameraTypes.put("SX777", "C765UZ");
	        OlympusCameraTypes.put("SX77A", "D555Z,C315Z");
	        OlympusCameraTypes.put("SX851", "C7070WZ");
	        OlympusCameraTypes.put("SX852", "C70Z,C7000Z");
	        OlympusCameraTypes.put("SX853", "SP500UZ");
	        OlympusCameraTypes.put("SX854", "SP310");
	        OlympusCameraTypes.put("SX855", "SP350");
	        OlympusCameraTypes.put("SX873", "SP320");
	        OlympusCameraTypes.put("SX875", "FE180/X745"); // (also D4330)
	        OlympusCameraTypes.put("SX876", "FE190/X750"); // (also D4327)

	        //   other brands
	        //    4MP9Q3", "Camera 4MP-9Q3'
	        //    4MP9T2", "BenQ DC C420 / Camera 4MP-9T2'
	        //    5MP9Q3", "Camera 5MP-9Q3" },
	        //    5MP9X9", "Camera 5MP-9X9" },
	        //   '5MP-9T'=> 'Camera 5MP-9T3" },
	        //   '5MP-9Y'=> 'Camera 5MP-9Y2" },
	        //   '6MP-9U'=> 'Camera 6MP-9U9" },
	        //    7MP9Q3", "Camera 7MP-9Q3" },
	        //   '8MP-9U'=> 'Camera 8MP-9U4" },
	        //    CE5330", "Acer CE-5330" },
	        //   'CP-853'=> 'Acer CP-8531" },
	        //    CS5531", "Acer CS5531" },
	        //    DC500 ", "SeaLife DC500" },
	        //    DC7370", "Camera 7MP-9GA" },
	        //    DC7371", "Camera 7MP-9GM" },
	        //    DC7371", "Hitachi HDC-751E" },
	        //    DC7375", "Hitachi HDC-763E / Rollei RCP-7330X / Ricoh Caplio RR770 / Vivitar ViviCam 7330" },
	        //   'DC E63'=> 'BenQ DC E63+" },
	        //   'DC P86'=> 'BenQ DC P860" },
	        //    DS5340", "Maginon Performic S5 / Premier 5MP-9M7" },
	        //    DS5341", "BenQ E53+ / Supra TCM X50 / Maginon X50 / Premier 5MP-9P8" },
	        //    DS5346", "Premier 5MP-9Q2" },
	        //    E500  ", "Konica Minolta DiMAGE E500" },
	        //    MAGINO", "Maginon X60" },
	        //    Mz60  ", "HP Photosmart Mz60" },
	        //    Q3DIGI", "Camera 5MP-9Q3" },
	        //    SLIMLI", "Supra Slimline X6" },
	        //    V8300s", "Vivitar V8300s" },
	    }
	}

	/**
	 * @author Drew Noakes http://drewnoakes.com
	 */
	public class DateUtil
	{
		
		public DateUtil() {
			
		}
	    private int[] _daysInMonth365 = new int[] {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

	    /**
	     * The offset (in milliseconds) to add to a MP4 date/time integer value to
	     * align with Java's Epoch.
	     */
	    private static final long EPOCH_1_JAN_1904 = -2082844800000L;

	    public boolean isValidDate(int year, int month, int day)
	    {
	        if (year < 1 || year > 9999 || month < 0 || month > 11)
	            return false;

	        int daysInMonth = _daysInMonth365[month];
	        if (month == 1)
	        {
	            boolean isLeapYear = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
	            if (isLeapYear)
	                daysInMonth++;
	        }

	        return day >= 1 && day <= daysInMonth;
	    }

	    public boolean isValidTime(int hours, int minutes, int seconds)
	    {
	        return hours >= 0 && hours < 24
	            && minutes >= 0 && minutes < 60
	            && seconds >= 0 && seconds < 60;
	    }

	    public Date get1Jan1904EpochDate(long seconds)
	    {
	        return new Date((seconds * 1000) + EPOCH_1_JAN_1904);
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link ExifImageDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifImageDescriptor extends ExifDescriptorBase<ExifImageDirectory>
	{
	    public ExifImageDescriptor(@NotNull ExifImageDirectory directory)
	    {
	        super(directory);
	    }
	}

	
	/**
	 * Describes One of several Exif directories.
	 *
	 * Holds information about image IFD's in a chain after the first. The first page is stored in IFD0.
	 * Currently, this only applied to multi-page TIFF images
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifImageDirectory extends ExifDirectoryBase
	{
	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    
	    {
	        addExifTagNames(_tagNameMap);
	    }

	    public ExifImageDirectory()
	    {
	        this.setDescriptor(new ExifImageDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Exif Image";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link ExifThumbnailDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifThumbnailDescriptor extends ExifDescriptorBase<ExifThumbnailDirectory>
	{
	    public ExifThumbnailDescriptor(@NotNull ExifThumbnailDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET:
	                return getThumbnailOffsetDescription();
	            case ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH:
	                return getThumbnailLengthDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getThumbnailLengthDescription()
	    {
	        String value = _directory.getString(ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH);
	        return value == null ? null : value + " bytes";
	    }

	    @Nullable
	    public String getThumbnailOffsetDescription()
	    {
	        String value = _directory.getString(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET);
	        return value == null ? null : value + " bytes";
	    }
	}


	/**
	 * One of several Exif directories.  Otherwise known as IFD1, this directory holds information about an embedded thumbnail image.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class ExifThumbnailDirectory extends ExifDirectoryBase
	{
	    /**
	     * The offset to thumbnail image bytes.
	     */
	    public static final int TAG_THUMBNAIL_OFFSET = 0x0201;
	    /**
	     * The size of the thumbnail image data in bytes.
	     */
	    public static final int TAG_THUMBNAIL_LENGTH = 0x0202;

	    /**
	     * @deprecated use {@link com.drew.metadata.exif.ExifDirectoryBase#TAG_COMPRESSION} instead.
	     */
	    @Deprecated
	    public static final int TAG_THUMBNAIL_COMPRESSION = 0x0103;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    
	    {
	        addExifTagNames(_tagNameMap);

	        _tagNameMap.put(TAG_THUMBNAIL_OFFSET, "Thumbnail Offset");
	        _tagNameMap.put(TAG_THUMBNAIL_LENGTH, "Thumbnail Length");
	    }

	    public ExifThumbnailDirectory()
	    {
	        this.setDescriptor(new ExifThumbnailDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Exif Thumbnail";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable String representations of tag values stored in a {@link OlympusEquipmentMakernoteDirectory}.
	 * <p>
	 * Some Description functions and the Extender and Lens types lists converted from Exiftool version 10.10 created by Phil Harvey
	 * http://www.sno.phy.queensu.ca/~phil/exiftool/
	 * lib\Image\ExifTool\Olympus.pm
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusEquipmentMakernoteDescriptor extends TagDescriptor<OlympusEquipmentMakernoteDirectory>
	{
	    public OlympusEquipmentMakernoteDescriptor(@NotNull OlympusEquipmentMakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case OlympusEquipmentMakernoteDirectory.TAG_EQUIPMENT_VERSION:
	                return getEquipmentVersionDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_CAMERA_TYPE_2:
	                return getCameraType2Description();
	            case OlympusEquipmentMakernoteDirectory.TAG_FOCAL_PLANE_DIAGONAL:
	                return getFocalPlaneDiagonalDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_BODY_FIRMWARE_VERSION:
	                return getBodyFirmwareVersionDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_LENS_TYPE:
	                return getLensTypeDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_LENS_FIRMWARE_VERSION:
	                return getLensFirmwareVersionDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_MAX_APERTURE_AT_MIN_FOCAL:
	                return getMaxApertureAtMinFocalDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_MAX_APERTURE_AT_MAX_FOCAL:
	                return getMaxApertureAtMaxFocalDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_MAX_APERTURE:
	                return getMaxApertureDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_LENS_PROPERTIES:
	                return getLensPropertiesDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_EXTENDER:
	                return getExtenderDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_FLASH_TYPE:
	                return getFlashTypeDescription();
	            case OlympusEquipmentMakernoteDirectory.TAG_FLASH_MODEL:
	                return getFlashModelDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getEquipmentVersionDescription()
	    {
	        return getVersionBytesDescription(OlympusEquipmentMakernoteDirectory.TAG_EQUIPMENT_VERSION, 4);
	    }

	    @Nullable
	    public String getCameraType2Description()
	    {
	        String cameratype = _directory.getString(OlympusEquipmentMakernoteDirectory.TAG_CAMERA_TYPE_2);
	        if(cameratype == null)
	            return null;
            OlympusMakernoteDirectory oldir = new OlympusMakernoteDirectory();
	        if(oldir.OlympusCameraTypes.containsKey(cameratype))
	            return oldir.OlympusCameraTypes.get(cameratype);

	        return cameratype;
	    }

	    @Nullable
	    public String getFocalPlaneDiagonalDescription()
	    {
	        return _directory.getString(OlympusEquipmentMakernoteDirectory.TAG_FOCAL_PLANE_DIAGONAL) + " mm";
	    }

	    @Nullable
	    public String getBodyFirmwareVersionDescription()
	    {
	        Integer value = _directory.getInteger(OlympusEquipmentMakernoteDirectory.TAG_BODY_FIRMWARE_VERSION);
	        if (value == null)
	            return null;

	        String hex = String.format("%04X", value);
	        return String.format("%s.%s",
	            hex.substring(0, hex.length() - 3),
	            hex.substring(hex.length() - 3));
	    }

	    @Nullable
	    public String getLensTypeDescription()
	    {
	        String str = _directory.getString(OlympusEquipmentMakernoteDirectory.TAG_LENS_TYPE);

	        if (str == null)
	            return null;

	        // The String contains six numbers:
	        //
	        // - Make
	        // - Unknown
	        // - Model
	        // - Sub-model
	        // - Unknown
	        // - Unknown
	        //
	        // Only the Make, Model and Sub-model are used to identify the lens type
	        String[] values = str.split(" ");

	        if (values.length < 6)
	            return null;

	        try {
	            int num1 = Integer.parseInt(values[0]);
	            int num2 = Integer.parseInt(values[2]);
	            int num3 = Integer.parseInt(values[3]);
	            return _olympusLensTypes.get(String.format("%X %02X %02X", num1, num2, num3));
	        } catch (NumberFormatException e) {
	            return null;
	        }
	    }

	    @Nullable
	    public String getLensFirmwareVersionDescription()
	    {
	        Integer value = _directory.getInteger(OlympusEquipmentMakernoteDirectory.TAG_LENS_FIRMWARE_VERSION);
	        if (value == null)
	            return null;

	        String hex = String.format("%04X", value);
	        return String.format("%s.%s",
	            hex.substring(0, hex.length() - 3),
	            hex.substring(hex.length() - 3));
	    }

	    @Nullable
	    public String getMaxApertureAtMinFocalDescription()
	    {
	        Integer value = _directory.getInteger(OlympusEquipmentMakernoteDirectory.TAG_MAX_APERTURE_AT_MIN_FOCAL);
	        if (value == null)
	            return null;

	        DecimalFormat format = new DecimalFormat("0.#");
	        return format.format(CalcMaxAperture(value));
	    }

	    @Nullable
	    public String getMaxApertureAtMaxFocalDescription()
	    {
	        Integer value = _directory.getInteger(OlympusEquipmentMakernoteDirectory.TAG_MAX_APERTURE_AT_MAX_FOCAL);
	        if (value == null)
	            return null;

	        DecimalFormat format = new DecimalFormat("0.#");
	        return format.format(CalcMaxAperture(value));
	    }

	    @Nullable
	    public String getMaxApertureDescription()
	    {
	        Integer value = _directory.getInteger(OlympusEquipmentMakernoteDirectory.TAG_MAX_APERTURE);
	        if (value == null)
	            return null;

	        DecimalFormat format = new DecimalFormat("0.#");
	        return format.format(CalcMaxAperture(value));
	    }

	    private double CalcMaxAperture(int value)
	    {
	        return Math.pow(Math.sqrt(2.00), value / 256.0);
	    }

	    @Nullable
	    public String getLensPropertiesDescription()
	    {
	        Integer value = _directory.getInteger(OlympusEquipmentMakernoteDirectory.TAG_LENS_PROPERTIES);
	        if (value == null)
	            return null;

	        return String.format("0x%04X", value);
	    }

	    @Nullable
	    public String getExtenderDescription()
	    {
	        String str = _directory.getString(OlympusEquipmentMakernoteDirectory.TAG_EXTENDER);

	        if (str == null)
	            return null;

	        // The String contains six numbers:
	        //
	        // - Make
	        // - Unknown
	        // - Model
	        // - Sub-model
	        // - Unknown
	        // - Unknown
	        //
	        // Only the Make and Model are used to identify the extender
	        String[] values = str.split(" ");

	        if (values.length < 6)
	            return null;

	        try {
	            int num1 = Integer.parseInt(values[0]);
	            int num2 = Integer.parseInt(values[2]);
	            String extenderType = String.format("%X %02X", num1, num2);
	            return _olympusExtenderTypes.get(extenderType);
	        } catch (NumberFormatException e) {
	            return null;
	        }
	    }

	    @Nullable
	    public String getFlashTypeDescription()
	    {
	        return getIndexedDescription(OlympusEquipmentMakernoteDirectory.TAG_FLASH_TYPE,
	            "None", null, "Simple E-System", "E-System");
	    }

	    @Nullable
	    public String getFlashModelDescription()
	    {
	        return getIndexedDescription(OlympusEquipmentMakernoteDirectory.TAG_FLASH_MODEL,
	            "None", "FL-20", "FL-50", "RF-11", "TF-22", "FL-36", "FL-50R", "FL-36R");
	    }

	    private final HashMap<String, String> _olympusLensTypes = new HashMap<String, String>();
	    private final HashMap<String, String> _olympusExtenderTypes = new HashMap<String, String>();

	     {
	        _olympusLensTypes.put("0 00 00", "None");
	        // Olympus lenses (also Kenko Tokina)
	        _olympusLensTypes.put("0 01 00", "Olympus Zuiko Digital ED 50mm F2.0 Macro");
	        _olympusLensTypes.put("0 01 01", "Olympus Zuiko Digital 40-150mm F3.5-4.5"); //8
	        _olympusLensTypes.put("0 01 10", "Olympus M.Zuiko Digital ED 14-42mm F3.5-5.6"); //PH (E-P1 pre-production)
	        _olympusLensTypes.put("0 02 00", "Olympus Zuiko Digital ED 150mm F2.0");
	        _olympusLensTypes.put("0 02 10", "Olympus M.Zuiko Digital 17mm F2.8 Pancake"); //PH (E-P1 pre-production)
	        _olympusLensTypes.put("0 03 00", "Olympus Zuiko Digital ED 300mm F2.8");
	        _olympusLensTypes.put("0 03 10", "Olympus M.Zuiko Digital ED 14-150mm F4.0-5.6 [II]"); //11 (The second version of this lens seems to have the same lens ID number as the first version #20)
	        _olympusLensTypes.put("0 04 10", "Olympus M.Zuiko Digital ED 9-18mm F4.0-5.6"); //11
	        _olympusLensTypes.put("0 05 00", "Olympus Zuiko Digital 14-54mm F2.8-3.5");
	        _olympusLensTypes.put("0 05 01", "Olympus Zuiko Digital Pro ED 90-250mm F2.8"); //9
	        _olympusLensTypes.put("0 05 10", "Olympus M.Zuiko Digital ED 14-42mm F3.5-5.6 L"); //11 (E-PL1)
	        _olympusLensTypes.put("0 06 00", "Olympus Zuiko Digital ED 50-200mm F2.8-3.5");
	        _olympusLensTypes.put("0 06 01", "Olympus Zuiko Digital ED 8mm F3.5 Fisheye"); //9
	        _olympusLensTypes.put("0 06 10", "Olympus M.Zuiko Digital ED 40-150mm F4.0-5.6"); //PH
	        _olympusLensTypes.put("0 07 00", "Olympus Zuiko Digital 11-22mm F2.8-3.5");
	        _olympusLensTypes.put("0 07 01", "Olympus Zuiko Digital 18-180mm F3.5-6.3"); //6
	        _olympusLensTypes.put("0 07 10", "Olympus M.Zuiko Digital ED 12mm F2.0"); //PH
	        _olympusLensTypes.put("0 08 01", "Olympus Zuiko Digital 70-300mm F4.0-5.6"); //7 (seen as release 1 - PH)
	        _olympusLensTypes.put("0 08 10", "Olympus M.Zuiko Digital ED 75-300mm F4.8-6.7"); //PH
	        _olympusLensTypes.put("0 09 10", "Olympus M.Zuiko Digital 14-42mm F3.5-5.6 II"); //PH (E-PL2)
	        _olympusLensTypes.put("0 10 01", "Kenko Tokina Reflex 300mm F6.3 MF Macro"); //20
	        _olympusLensTypes.put("0 10 10", "Olympus M.Zuiko Digital ED 12-50mm F3.5-6.3 EZ"); //PH
	        _olympusLensTypes.put("0 11 10", "Olympus M.Zuiko Digital 45mm F1.8"); //17
	        _olympusLensTypes.put("0 12 10", "Olympus M.Zuiko Digital ED 60mm F2.8 Macro"); //20
	        _olympusLensTypes.put("0 13 10", "Olympus M.Zuiko Digital 14-42mm F3.5-5.6 II R"); //PH/20
	        _olympusLensTypes.put("0 14 10", "Olympus M.Zuiko Digital ED 40-150mm F4.0-5.6 R"); //19
	        // '0 14 10.1", "Olympus M.Zuiko Digital ED 14-150mm F4.0-5.6 II"); //11 (questionable & unconfirmed -- all samples I can find are '0 3 10' - PH)
	        _olympusLensTypes.put("0 15 00", "Olympus Zuiko Digital ED 7-14mm F4.0");
	        _olympusLensTypes.put("0 15 10", "Olympus M.Zuiko Digital ED 75mm F1.8"); //PH
	        _olympusLensTypes.put("0 16 10", "Olympus M.Zuiko Digital 17mm F1.8"); //20
	        _olympusLensTypes.put("0 17 00", "Olympus Zuiko Digital Pro ED 35-100mm F2.0"); //7
	        _olympusLensTypes.put("0 18 00", "Olympus Zuiko Digital 14-45mm F3.5-5.6");
	        _olympusLensTypes.put("0 18 10", "Olympus M.Zuiko Digital ED 75-300mm F4.8-6.7 II"); //20
	        _olympusLensTypes.put("0 19 10", "Olympus M.Zuiko Digital ED 12-40mm F2.8 Pro"); //PH
	        _olympusLensTypes.put("0 20 00", "Olympus Zuiko Digital 35mm F3.5 Macro"); //9
	        _olympusLensTypes.put("0 20 10", "Olympus M.Zuiko Digital ED 40-150mm F2.8 Pro"); //20
	        _olympusLensTypes.put("0 21 10", "Olympus M.Zuiko Digital ED 14-42mm F3.5-5.6 EZ"); //20
	        _olympusLensTypes.put("0 22 00", "Olympus Zuiko Digital 17.5-45mm F3.5-5.6"); //9
	        _olympusLensTypes.put("0 22 10", "Olympus M.Zuiko Digital 25mm F1.8"); //20
	        _olympusLensTypes.put("0 23 00", "Olympus Zuiko Digital ED 14-42mm F3.5-5.6"); //PH
	        _olympusLensTypes.put("0 23 10", "Olympus M.Zuiko Digital ED 7-14mm F2.8 Pro"); //20
	        _olympusLensTypes.put("0 24 00", "Olympus Zuiko Digital ED 40-150mm F4.0-5.6"); //PH
	        _olympusLensTypes.put("0 24 10", "Olympus M.Zuiko Digital ED 300mm F4.0 IS Pro"); //20
	        _olympusLensTypes.put("0 25 10", "Olympus M.Zuiko Digital ED 8mm F1.8 Fisheye Pro"); //20
	        _olympusLensTypes.put("0 30 00", "Olympus Zuiko Digital ED 50-200mm F2.8-3.5 SWD"); //7
	        _olympusLensTypes.put("0 31 00", "Olympus Zuiko Digital ED 12-60mm F2.8-4.0 SWD"); //7
	        _olympusLensTypes.put("0 32 00", "Olympus Zuiko Digital ED 14-35mm F2.0 SWD"); //PH
	        _olympusLensTypes.put("0 33 00", "Olympus Zuiko Digital 25mm F2.8"); //PH
	        _olympusLensTypes.put("0 34 00", "Olympus Zuiko Digital ED 9-18mm F4.0-5.6"); //7
	        _olympusLensTypes.put("0 35 00", "Olympus Zuiko Digital 14-54mm F2.8-3.5 II"); //PH
	        // Sigma lenses
	        _olympusLensTypes.put("1 01 00", "Sigma 18-50mm F3.5-5.6 DC"); //8
	        _olympusLensTypes.put("1 01 10", "Sigma 30mm F2.8 EX DN"); //20
	        _olympusLensTypes.put("1 02 00", "Sigma 55-200mm F4.0-5.6 DC");
	        _olympusLensTypes.put("1 02 10", "Sigma 19mm F2.8 EX DN"); //20
	        _olympusLensTypes.put("1 03 00", "Sigma 18-125mm F3.5-5.6 DC");
	        _olympusLensTypes.put("1 03 10", "Sigma 30mm F2.8 DN | A"); //20
	        _olympusLensTypes.put("1 04 00", "Sigma 18-125mm F3.5-5.6 DC"); //7
	        _olympusLensTypes.put("1 04 10", "Sigma 19mm F2.8 DN | A"); //20
	        _olympusLensTypes.put("1 05 00", "Sigma 30mm F1.4 EX DC HSM"); //10
	        _olympusLensTypes.put("1 05 10", "Sigma 60mm F2.8 DN | A"); //20
	        _olympusLensTypes.put("1 06 00", "Sigma APO 50-500mm F4.0-6.3 EX DG HSM"); //6
	        _olympusLensTypes.put("1 07 00", "Sigma Macro 105mm F2.8 EX DG"); //PH
	        _olympusLensTypes.put("1 08 00", "Sigma APO Macro 150mm F2.8 EX DG HSM"); //PH
	        _olympusLensTypes.put("1 09 00", "Sigma 18-50mm F2.8 EX DC Macro"); //20
	        _olympusLensTypes.put("1 10 00", "Sigma 24mm F1.8 EX DG Aspherical Macro"); //PH
	        _olympusLensTypes.put("1 11 00", "Sigma APO 135-400mm F4.5-5.6 DG"); //11
	        _olympusLensTypes.put("1 12 00", "Sigma APO 300-800mm F5.6 EX DG HSM"); //11
	        _olympusLensTypes.put("1 13 00", "Sigma 30mm F1.4 EX DC HSM"); //11
	        _olympusLensTypes.put("1 14 00", "Sigma APO 50-500mm F4.0-6.3 EX DG HSM"); //11
	        _olympusLensTypes.put("1 15 00", "Sigma 10-20mm F4.0-5.6 EX DC HSM"); //11
	        _olympusLensTypes.put("1 16 00", "Sigma APO 70-200mm F2.8 II EX DG Macro HSM"); //11
	        _olympusLensTypes.put("1 17 00", "Sigma 50mm F1.4 EX DG HSM"); //11
	        // Panasonic/Leica lenses
	        _olympusLensTypes.put("2 01 00", "Leica D Vario Elmarit 14-50mm F2.8-3.5 Asph."); //11
	        _olympusLensTypes.put("2 01 10", "Lumix G Vario 14-45mm F3.5-5.6 Asph. Mega OIS"); //16
	        _olympusLensTypes.put("2 02 00", "Leica D Summilux 25mm F1.4 Asph."); //11
	        _olympusLensTypes.put("2 02 10", "Lumix G Vario 45-200mm F4.0-5.6 Mega OIS"); //16
	        _olympusLensTypes.put("2 03 00", "Leica D Vario Elmar 14-50mm F3.8-5.6 Asph. Mega OIS"); //11
	        _olympusLensTypes.put("2 03 01", "Leica D Vario Elmar 14-50mm F3.8-5.6 Asph."); //14 (L10 kit)
	        _olympusLensTypes.put("2 03 10", "Lumix G Vario HD 14-140mm F4.0-5.8 Asph. Mega OIS"); //16
	        _olympusLensTypes.put("2 04 00", "Leica D Vario Elmar 14-150mm F3.5-5.6"); //13
	        _olympusLensTypes.put("2 04 10", "Lumix G Vario 7-14mm F4.0 Asph."); //PH (E-P1 pre-production)
	        _olympusLensTypes.put("2 05 10", "Lumix G 20mm F1.7 Asph."); //16
	        _olympusLensTypes.put("2 06 10", "Leica DG Macro-Elmarit 45mm F2.8 Asph. Mega OIS"); //PH
	        _olympusLensTypes.put("2 07 10", "Lumix G Vario 14-42mm F3.5-5.6 Asph. Mega OIS"); //20
	        _olympusLensTypes.put("2 08 10", "Lumix G Fisheye 8mm F3.5"); //PH
	        _olympusLensTypes.put("2 09 10", "Lumix G Vario 100-300mm F4.0-5.6 Mega OIS"); //11
	        _olympusLensTypes.put("2 10 10", "Lumix G 14mm F2.5 Asph."); //17
	        _olympusLensTypes.put("2 11 10", "Lumix G 12.5mm F12 3D"); //20 (H-FT012)
	        _olympusLensTypes.put("2 12 10", "Leica DG Summilux 25mm F1.4 Asph."); //20
	        _olympusLensTypes.put("2 13 10", "Lumix G X Vario PZ 45-175mm F4.0-5.6 Asph. Power OIS"); //20
	        _olympusLensTypes.put("2 14 10", "Lumix G X Vario PZ 14-42mm F3.5-5.6 Asph. Power OIS"); //20
	        _olympusLensTypes.put("2 15 10", "Lumix G X Vario 12-35mm F2.8 Asph. Power OIS"); //PH
	        _olympusLensTypes.put("2 16 10", "Lumix G Vario 45-150mm F4.0-5.6 Asph. Mega OIS"); //20
	        _olympusLensTypes.put("2 17 10", "Lumix G X Vario 35-100mm F2.8 Power OIS"); //PH
	        _olympusLensTypes.put("2 18 10", "Lumix G Vario 14-42mm F3.5-5.6 II Asph. Mega OIS"); //20
	        _olympusLensTypes.put("2 19 10", "Lumix G Vario 14-140mm F3.5-5.6 Asph. Power OIS"); //20
	        _olympusLensTypes.put("2 20 10", "Lumix G Vario 12-32mm F3.5-5.6 Asph. Mega OIS"); //20
	        _olympusLensTypes.put("2 21 10", "Leica DG Nocticron 42.5mm F1.2 Asph. Power OIS"); //20
	        _olympusLensTypes.put("2 22 10", "Leica DG Summilux 15mm F1.7 Asph."); //20
	        // '2 23 10", "Lumix G Vario 35-100mm F4.0-5.6 Asph. Mega OIS"); //20 (guess)
	        _olympusLensTypes.put("2 24 10", "Lumix G Macro 30mm F2.8 Asph. Mega OIS"); //20
	        _olympusLensTypes.put("2 25 10", "Lumix G 42.5mm F1.7 Asph. Power OIS"); //20
	        _olympusLensTypes.put("3 01 00", "Leica D Vario Elmarit 14-50mm F2.8-3.5 Asph."); //11
	        _olympusLensTypes.put("3 02 00", "Leica D Summilux 25mm F1.4 Asph."); //11
	        // Tamron lenses
	        _olympusLensTypes.put("5 01 10", "Tamron 14-150mm F3.5-5.8 Di III"); //20 (model C001)


	        _olympusExtenderTypes.put("0 00", "None");
	        _olympusExtenderTypes.put("0 04", "Olympus Zuiko Digital EC-14 1.4x Teleconverter");
	        _olympusExtenderTypes.put("0 08", "Olympus EX-25 Extension Tube");
	        _olympusExtenderTypes.put("0 10", "Olympus Zuiko Digital EC-20 2.0x Teleconverter");
	    }
	}

	
	/**
	 * The Olympus equipment makernote is used by many manufacturers (Epson, Konica, Minolta and Agfa...), and as such contains some tags
	 * that appear specific to those manufacturers.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusEquipmentMakernoteDirectory extends Directory
	{
	    public static final int TAG_EQUIPMENT_VERSION = 0x0000;
	    public static final int TAG_CAMERA_TYPE_2 = 0x0100;
	    public static final int TAG_SERIAL_NUMBER = 0x0101;

	    public static final int TAG_INTERNAL_SERIAL_NUMBER = 0x0102;
	    public static final int TAG_FOCAL_PLANE_DIAGONAL = 0x0103;
	    public static final int TAG_BODY_FIRMWARE_VERSION = 0x0104;

	    public static final int TAG_LENS_TYPE = 0x0201;
	    public static final int TAG_LENS_SERIAL_NUMBER = 0x0202;
	    public static final int TAG_LENS_MODEL = 0x0203;
	    public static final int TAG_LENS_FIRMWARE_VERSION = 0x0204;
	    public static final int TAG_MAX_APERTURE_AT_MIN_FOCAL = 0x0205;
	    public static final int TAG_MAX_APERTURE_AT_MAX_FOCAL = 0x0206;
	    public static final int TAG_MIN_FOCAL_LENGTH = 0x0207;
	    public static final int TAG_MAX_FOCAL_LENGTH = 0x0208;
	    public static final int TAG_MAX_APERTURE = 0x020A;
	    public static final int TAG_LENS_PROPERTIES = 0x020B;

	    public static final int TAG_EXTENDER = 0x0301;
	    public static final int TAG_EXTENDER_SERIAL_NUMBER = 0x0302;
	    public static final int TAG_EXTENDER_MODEL = 0x0303;
	    public static final int TAG_EXTENDER_FIRMWARE_VERSION = 0x0304;

	    public static final int TAG_CONVERSION_LENS = 0x0403;

	    public static final int TAG_FLASH_TYPE = 0x1000;
	    public static final int TAG_FLASH_MODEL = 0x1001;
	    public static final int TAG_FLASH_FIRMWARE_VERSION = 0x1002;
	    public static final int TAG_FLASH_SERIAL_NUMBER = 0x1003;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TAG_EQUIPMENT_VERSION, "Equipment Version");
	        _tagNameMap.put(TAG_CAMERA_TYPE_2, "Camera Type 2");
	        _tagNameMap.put(TAG_SERIAL_NUMBER, "Serial Number");
	        _tagNameMap.put(TAG_INTERNAL_SERIAL_NUMBER, "Internal Serial Number");
	        _tagNameMap.put(TAG_FOCAL_PLANE_DIAGONAL, "Focal Plane Diagonal");
	        _tagNameMap.put(TAG_BODY_FIRMWARE_VERSION, "Body Firmware Version");
	        _tagNameMap.put(TAG_LENS_TYPE, "Lens Type");
	        _tagNameMap.put(TAG_LENS_SERIAL_NUMBER, "Lens Serial Number");
	        _tagNameMap.put(TAG_LENS_MODEL, "Lens Model");
	        _tagNameMap.put(TAG_LENS_FIRMWARE_VERSION, "Lens Firmware Version");
	        _tagNameMap.put(TAG_MAX_APERTURE_AT_MIN_FOCAL, "Max Aperture At Min Focal");
	        _tagNameMap.put(TAG_MAX_APERTURE_AT_MAX_FOCAL, "Max Aperture At Max Focal");
	        _tagNameMap.put(TAG_MIN_FOCAL_LENGTH, "Min Focal Length");
	        _tagNameMap.put(TAG_MAX_FOCAL_LENGTH, "Max Focal Length");
	        _tagNameMap.put(TAG_MAX_APERTURE, "Max Aperture");
	        _tagNameMap.put(TAG_LENS_PROPERTIES, "Lens Properties");
	        _tagNameMap.put(TAG_EXTENDER, "Extender");
	        _tagNameMap.put(TAG_EXTENDER_SERIAL_NUMBER, "Extender Serial Number");
	        _tagNameMap.put(TAG_EXTENDER_MODEL, "Extender Model");
	        _tagNameMap.put(TAG_EXTENDER_FIRMWARE_VERSION, "Extender Firmware Version");
	        _tagNameMap.put(TAG_CONVERSION_LENS, "Conversion Lens");
	        _tagNameMap.put(TAG_FLASH_TYPE, "Flash Type");
	        _tagNameMap.put(TAG_FLASH_MODEL, "Flash Model");
	        _tagNameMap.put(TAG_FLASH_FIRMWARE_VERSION, "Flash Firmware Version");
	        _tagNameMap.put(TAG_FLASH_SERIAL_NUMBER, "Flash Serial Number");
	    }

	    public OlympusEquipmentMakernoteDirectory()
	    {
	        this.setDescriptor(new OlympusEquipmentMakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Olympus Equipment";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable String representations of tag values stored in a {@link OlympusCameraSettingsMakernoteDirectory}.
	 * <p>
	 * Some Description functions and the Extender and Lens types lists converted from Exiftool version 10.10 created by Phil Harvey
	 * http://www.sno.phy.queensu.ca/~phil/exiftool/
	 * lib\Image\ExifTool\Olympus.pm
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusCameraSettingsMakernoteDescriptor extends TagDescriptor<OlympusCameraSettingsMakernoteDirectory>
	{
	    public OlympusCameraSettingsMakernoteDescriptor(@NotNull OlympusCameraSettingsMakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case OlympusCameraSettingsMakernoteDirectory.TagCameraSettingsVersion:
	                return getCameraSettingsVersionDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagPreviewImageValid:
	                return getPreviewImageValidDescription();

	            case OlympusCameraSettingsMakernoteDirectory.TagExposureMode:
	                return getExposureModeDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagAeLock:
	                return getAeLockDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagMeteringMode:
	                return getMeteringModeDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagExposureShift:
	                return getExposureShiftDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagNdFilter:
	                return getNdFilterDescription();

	            case OlympusCameraSettingsMakernoteDirectory.TagMacroMode:
	                return getMacroModeDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagFocusMode:
	                return getFocusModeDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagFocusProcess:
	                return getFocusProcessDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagAfSearch:
	                return getAfSearchDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagAfAreas:
	                return getAfAreasDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagAfPointSelected:
	                return getAfPointSelectedDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagAfFineTune:
	                return getAfFineTuneDescription();

	            case OlympusCameraSettingsMakernoteDirectory.TagFlashMode:
	                return getFlashModeDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagFlashRemoteControl:
	                return getFlashRemoteControlDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagFlashControlMode:
	                return getFlashControlModeDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagFlashIntensity:
	                return getFlashIntensityDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagManualFlashStrength:
	                return getManualFlashStrengthDescription();

	            case OlympusCameraSettingsMakernoteDirectory.TagWhiteBalance2:
	                return getWhiteBalance2Description();
	            case OlympusCameraSettingsMakernoteDirectory.TagWhiteBalanceTemperature:
	                return getWhiteBalanceTemperatureDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagCustomSaturation:
	                return getCustomSaturationDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagModifiedSaturation:
	                return getModifiedSaturationDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagContrastSetting:
	                return getContrastSettingDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagSharpnessSetting:
	                return getSharpnessSettingDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagColorSpace:
	                return getColorSpaceDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagSceneMode:
	                return getSceneModeDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagNoiseReduction:
	                return getNoiseReductionDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagDistortionCorrection:
	                return getDistortionCorrectionDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagShadingCompensation:
	                return getShadingCompensationDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagGradation:
	                return getGradationDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagPictureMode:
	                return getPictureModeDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagPictureModeSaturation:
	                return getPictureModeSaturationDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagPictureModeContrast:
	                return getPictureModeContrastDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagPictureModeSharpness:
	                return getPictureModeSharpnessDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagPictureModeBWFilter:
	                return getPictureModeBWFilterDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagPictureModeTone:
	                return getPictureModeToneDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagNoiseFilter:
	                return getNoiseFilterDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagArtFilter:
	                return getArtFilterDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagMagicFilter:
	                return getMagicFilterDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagPictureModeEffect:
	                return getPictureModeEffectDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagToneLevel:
	                return getToneLevelDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagArtFilterEffect:
	                return getArtFilterEffectDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagColorCreatorEffect:
	                return getColorCreatorEffectDescription();

	            case OlympusCameraSettingsMakernoteDirectory.TagDriveMode:
	                return getDriveModeDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagPanoramaMode:
	                return getPanoramaModeDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagImageQuality2:
	                return getImageQuality2Description();
	            case OlympusCameraSettingsMakernoteDirectory.TagImageStabilization:
	                return getImageStabilizationDescription();

	            case OlympusCameraSettingsMakernoteDirectory.TagStackedImage:
	                return getStackedImageDescription();

	            case OlympusCameraSettingsMakernoteDirectory.TagManometerPressure:
	                return getManometerPressureDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagManometerReading:
	                return getManometerReadingDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagExtendedWBDetect:
	                return getExtendedWBDetectDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagRollAngle:
	                return getRollAngleDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagPitchAngle:
	                return getPitchAngleDescription();
	            case OlympusCameraSettingsMakernoteDirectory.TagDateTimeUtc:
	                return getDateTimeUTCDescription();

	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getCameraSettingsVersionDescription()
	    {
	        return getVersionBytesDescription(OlympusCameraSettingsMakernoteDirectory.TagCameraSettingsVersion, 4);
	    }

	    @Nullable
	    public String getPreviewImageValidDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagPreviewImageValid,
	            "No", "Yes");
	    }

	    @Nullable
	    public String getExposureModeDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagExposureMode, 1,
	            "Manual", "Program", "Aperture-priority AE", "Shutter speed priority", "Program-shift");
	    }

	    @Nullable
	    public String getAeLockDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagAeLock,
	            "Off", "On");
	    }

	    @Nullable
	    public String getMeteringModeDescription()
	    {
	        Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagMeteringMode);
	        if (value == null)
	            return null;

	        switch (value) {
	            case 2:
	                return "Center-weighted average";
	            case 3:
	                return "Spot";
	            case 5:
	                return "ESP";
	            case 261:
	                return "Pattern+AF";
	            case 515:
	                return "Spot+Highlight control";
	            case 1027:
	                return "Spot+Shadow control";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getExposureShiftDescription()
	    {
	        return getRationalOrDoubleString(OlympusCameraSettingsMakernoteDirectory.TagExposureShift);
	    }

	    @Nullable
	    public String getNdFilterDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagNdFilter, "Off", "On");
	    }

	    @Nullable
	    public String getMacroModeDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagMacroMode, "Off", "On", "Super Macro");
	    }

	    @Nullable
	    public String getFocusModeDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagFocusMode);
	        if (values == null) {
	            // check if it's only one value long also
	            Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagFocusMode);
	            if (value == null)
	                return null;

	            values = new int[]{value};
	        }

	        if (values.length == 0)
	            return null;

	        StringBuilder sb = new StringBuilder();
	        switch (values[0]) {
	            case 0:
	                sb.append("Single AF");
	                break;
	            case 1:
	                sb.append("Sequential shooting AF");
	                break;
	            case 2:
	                sb.append("Continuous AF");
	                break;
	            case 3:
	                sb.append("Multi AF");
	                break;
	            case 4:
	                sb.append("Face detect");
	                break;
	            case 10:
	                sb.append("MF");
	                break;
	            default:
	                sb.append("Unknown (" + values[0] + ")");
	                break;
	        }

	        if (values.length > 1) {
	            sb.append("; ");
	            int value1 = values[1];

	            if (value1 == 0) {
	                sb.append("(none)");
	            } else {
	                if (( value1       & 1) > 0) sb.append("S-AF, ");
	                if (((value1 >> 2) & 1) > 0) sb.append("C-AF, ");
	                if (((value1 >> 4) & 1) > 0) sb.append("MF, ");
	                if (((value1 >> 5) & 1) > 0) sb.append("Face detect, ");
	                if (((value1 >> 6) & 1) > 0) sb.append("Imager AF, ");
	                if (((value1 >> 7) & 1) > 0) sb.append("Live View Magnification Frame, ");
	                if (((value1 >> 8) & 1) > 0) sb.append("AF sensor, ");

	                sb.setLength(sb.length() - 2);
	            }
	        }

	        return sb.toString();
	    }

	    @Nullable
	    public String getFocusProcessDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagFocusProcess);
	        if (values == null) {
	            // check if it's only one value long also
	            Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagFocusProcess);
	            if (value == null)
	                return null;

	            values = new int[]{value};
	        }

	        if (values.length == 0)
	            return null;

	        StringBuilder sb = new StringBuilder();

	        switch (values[0]) {
	            case 0:
	                sb.append("AF not used");
	                break;
	            case 1:
	                sb.append("AF used");
	                break;
	            default:
	                sb.append("Unknown (" + values[0] + ")");
	                break;
	        }

	        if (values.length > 1)
	            sb.append("; " + values[1]);

	        return sb.toString();
	    }

	    @Nullable
	    public String getAfSearchDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagAfSearch, "Not Ready", "Ready");
	    }

	    /** coordinates range from 0 to 255 */
	    @Nullable
	    public String getAfAreasDescription()
	    {
	        Object obj = _directory.getObject(OlympusCameraSettingsMakernoteDirectory.TagAfAreas);
	        if (obj == null || !(obj instanceof long[]))
	            return null;

	        StringBuilder sb = new StringBuilder();
	        for (long point : (long[]) obj) {
	            if (point == 0L)
	                continue;
	            if (sb.length() != 0)
	                sb.append(", ");

	            if (point == 0x36794285L)
	                sb.append("Left ");
	            else if (point == 0x79798585L)
	                sb.append("Center ");
	            else if (point == 0xBD79C985L)
	                sb.append("Right ");

	            sb.append(String.format("(%d/255,%d/255)-(%d/255,%d/255)",
	                (point >> 24) & 0xFF,
	                (point >> 16) & 0xFF,
	                (point >> 8) & 0xFF,
	                point & 0xFF));
	        }

	        return sb.length() == 0 ? null : sb.toString();
	    }

	    /** coordinates expressed as a percent */
	    @Nullable
	    public String getAfPointSelectedDescription()
	    {
	        Rational[] values = _directory.getRationalArray(OlympusCameraSettingsMakernoteDirectory.TagAfPointSelected);
	        if (values == null)
	            return "n/a";

	        if (values.length < 4)
	            return null;

	        int index = 0;
	        if (values.length == 5 && values[0].longValue() == 0)
	            index = 1;

	        int p1 = (int)(values[index].doubleValue() * 100);
	        int p2 = (int)(values[index + 1].doubleValue() * 100);
	        int p3 = (int)(values[index + 2].doubleValue() * 100);
	        int p4 = (int)(values[index + 3].doubleValue() * 100);

	        if(p1 + p2 + p3 + p4 == 0)
	            return "n/a";

	        return String.format("(%d%%,%d%%) (%d%%,%d%%)", p1, p2, p3, p4);
	    }

	    @Nullable
	    public String getAfFineTuneDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagAfFineTune, "Off", "On");
	    }

	    @Nullable
	    public String getFlashModeDescription()
	    {
	        Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagFlashMode);
	        if (value == null)
	            return null;

	        if (value == 0)
	            return "Off";

	        StringBuilder sb = new StringBuilder();
	        int v = value;

	        if (( v       & 1) != 0) sb.append("On, ");
	        if (((v >> 1) & 1) != 0) sb.append("Fill-in, ");
	        if (((v >> 2) & 1) != 0) sb.append("Red-eye, ");
	        if (((v >> 3) & 1) != 0) sb.append("Slow-sync, ");
	        if (((v >> 4) & 1) != 0) sb.append("Forced On, ");
	        if (((v >> 5) & 1) != 0) sb.append("2nd Curtain, ");

	        return sb.substring(0, sb.length() - 2);
	    }

	    @Nullable
	    public String getFlashRemoteControlDescription()
	    {
	        Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagFlashRemoteControl);
	        if (value == null)
	            return null;

	        switch (value) {
	            case 0:
	                return "Off";
	            case 0x01:
	                return "Channel 1, Low";
	            case 0x02:
	                return "Channel 2, Low";
	            case 0x03:
	                return "Channel 3, Low";
	            case 0x04:
	                return "Channel 4, Low";
	            case 0x09:
	                return "Channel 1, Mid";
	            case 0x0a:
	                return "Channel 2, Mid";
	            case 0x0b:
	                return "Channel 3, Mid";
	            case 0x0c:
	                return "Channel 4, Mid";
	            case 0x11:
	                return "Channel 1, High";
	            case 0x12:
	                return "Channel 2, High";
	            case 0x13:
	                return "Channel 3, High";
	            case 0x14:
	                return "Channel 4, High";

	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    /** 3 or 4 values */
	    @Nullable
	    public String getFlashControlModeDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagFlashControlMode);
	        if (values == null)
	            return null;

	        if (values.length == 0)
	            return null;

	        StringBuilder sb = new StringBuilder();

	        switch (values[0]) {
	            case 0:
	                sb.append("Off");
	                break;
	            case 3:
	                sb.append("TTL");
	                break;
	            case 4:
	                sb.append("Auto");
	                break;
	            case 5:
	                sb.append("Manual");
	                break;
	            default:
	                sb.append("Unknown (").append(values[0]).append(")");
	                break;
	        }

	        for (int i = 1; i < values.length; i++)
	            sb.append("; ").append(values[i]);

	        return sb.toString();
	    }

	    /** 3 or 4 values */
	    @Nullable
	    public String getFlashIntensityDescription()
	    {
	        Rational[] values = _directory.getRationalArray(OlympusCameraSettingsMakernoteDirectory.TagFlashIntensity);
	        if (values == null || values.length == 0)
	            return null;

	        if (values.length == 3) {
	            if (values[0].getDenominator() == 0 && values[1].getDenominator() == 0 && values[2].getDenominator() == 0)
	                return "n/a";
	        } else if (values.length == 4) {
	            if (values[0].getDenominator() == 0 && values[1].getDenominator() == 0 && values[2].getDenominator() == 0 && values[3].getDenominator() == 0)
	                return "n/a (x4)";
	        }

	        StringBuilder sb = new StringBuilder();
	        for (Rational t : values)
	            sb.append(t).append(", ");

	        return sb.substring(0, sb.length() - 2);
	    }

	    @Nullable
	    public String getManualFlashStrengthDescription()
	    {
	        Rational[] values = _directory.getRationalArray(OlympusCameraSettingsMakernoteDirectory.TagManualFlashStrength);
	        if (values == null || values.length == 0)
	            return "n/a";

	        if (values.length == 3) {
	            if (values[0].getDenominator() == 0 && values[1].getDenominator() == 0 && values[2].getDenominator() == 0)
	                return "n/a";
	        } else if (values.length == 4) {
	            if (values[0].getDenominator() == 0 && values[1].getDenominator() == 0 && values[2].getDenominator() == 0 && values[3].getDenominator() == 0)
	                return "n/a (x4)";
	        }

	        StringBuilder sb = new StringBuilder();
	        for (Rational t : values)
	            sb.append(t).append(", ");

	        return sb.substring(0, sb.length() - 2);
	    }

	    @Nullable
	    public String getWhiteBalance2Description()
	    {
	        Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagWhiteBalance2);
	        if (value == null)
	            return null;

	        switch (value) {
	            case 0:
	                return "Auto";
	            case 1:
	                return "Auto (Keep Warm Color Off)";
	            case 16:
	                return "7500K (Fine Weather with Shade)";
	            case 17:
	                return "6000K (Cloudy)";
	            case 18:
	                return "5300K (Fine Weather)";
	            case 20:
	                return "3000K (Tungsten light)";
	            case 21:
	                return "3600K (Tungsten light-like)";
	            case 22:
	                return "Auto Setup";
	            case 23:
	                return "5500K (Flash)";
	            case 33:
	                return "6600K (Daylight fluorescent)";
	            case 34:
	                return "4500K (Neutral white fluorescent)";
	            case 35:
	                return "4000K (Cool white fluorescent)";
	            case 36:
	                return "White Fluorescent";
	            case 48:
	                return "3600K (Tungsten light-like)";
	            case 67:
	                return "Underwater";
	            case 256:
	                return "One Touch WB 1";
	            case 257:
	                return "One Touch WB 2";
	            case 258:
	                return "One Touch WB 3";
	            case 259:
	                return "One Touch WB 4";
	            case 512:
	                return "Custom WB 1";
	            case 513:
	                return "Custom WB 2";
	            case 514:
	                return "Custom WB 3";
	            case 515:
	                return "Custom WB 4";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getWhiteBalanceTemperatureDescription()
	    {
	        Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagWhiteBalanceTemperature);
	        if (value == null)
	            return null;
	        if (value == 0)
	            return "Auto";
	        return value.toString();
	    }

	    @Nullable
	    public String getCustomSaturationDescription()
	    {
	        // TODO: if model is /^E-1\b/  then
	        // $a-=$b; $c-=$b;
	        // return "CS$a (min CS0, max CS$c)"
	        return getValueMinMaxDescription(OlympusCameraSettingsMakernoteDirectory.TagCustomSaturation);
	    }

	    @Nullable
	    public String getModifiedSaturationDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagModifiedSaturation,
	            "Off", "CM1 (Red Enhance)", "CM2 (Green Enhance)", "CM3 (Blue Enhance)", "CM4 (Skin Tones)");
	    }

	    @Nullable
	    public String getContrastSettingDescription()
	    {
	        return getValueMinMaxDescription(OlympusCameraSettingsMakernoteDirectory.TagContrastSetting);
	    }

	    @Nullable
	    public String getSharpnessSettingDescription()
	    {
	        return getValueMinMaxDescription(OlympusCameraSettingsMakernoteDirectory.TagSharpnessSetting);
	    }

	    @Nullable
	    public String getColorSpaceDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagColorSpace,
	            "sRGB", "Adobe RGB", "Pro Photo RGB");
	    }

	    @Nullable
	    public String getSceneModeDescription()
	    {
	        Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagSceneMode);
	        if (value == null)
	            return null;

	        switch (value) {
	            case 0:
	                return "Standard";
	            case 6:
	                return "Auto";
	            case 7:
	                return "Sport";
	            case 8:
	                return "Portrait";
	            case 9:
	                return "Landscape+Portrait";
	            case 10:
	                return "Landscape";
	            case 11:
	                return "Night Scene";
	            case 12:
	                return "Self Portrait";
	            case 13:
	                return "Panorama";
	            case 14:
	                return "2 in 1";
	            case 15:
	                return "Movie";
	            case 16:
	                return "Landscape+Portrait";
	            case 17:
	                return "Night+Portrait";
	            case 18:
	                return "Indoor";
	            case 19:
	                return "Fireworks";
	            case 20:
	                return "Sunset";
	            case 21:
	                return "Beauty Skin";
	            case 22:
	                return "Macro";
	            case 23:
	                return "Super Macro";
	            case 24:
	                return "Food";
	            case 25:
	                return "Documents";
	            case 26:
	                return "Museum";
	            case 27:
	                return "Shoot & Select";
	            case 28:
	                return "Beach & Snow";
	            case 29:
	                return "Self Portrait+Timer";
	            case 30:
	                return "Candle";
	            case 31:
	                return "Available Light";
	            case 32:
	                return "Behind Glass";
	            case 33:
	                return "My Mode";
	            case 34:
	                return "Pet";
	            case 35:
	                return "Underwater Wide1";
	            case 36:
	                return "Underwater Macro";
	            case 37:
	                return "Shoot & Select1";
	            case 38:
	                return "Shoot & Select2";
	            case 39:
	                return "High Key";
	            case 40:
	                return "Digital Image Stabilization";
	            case 41:
	                return "Auction";
	            case 42:
	                return "Beach";
	            case 43:
	                return "Snow";
	            case 44:
	                return "Underwater Wide2";
	            case 45:
	                return "Low Key";
	            case 46:
	                return "Children";
	            case 47:
	                return "Vivid";
	            case 48:
	                return "Nature Macro";
	            case 49:
	                return "Underwater Snapshot";
	            case 50:
	                return "Shooting Guide";
	            case 54:
	                return "Face Portrait";
	            case 57:
	                return "Bulb";
	            case 59:
	                return "Smile Shot";
	            case 60:
	                return "Quick Shutter";
	            case 63:
	                return "Slow Shutter";
	            case 64:
	                return "Bird Watching";
	            case 65:
	                return "Multiple Exposure";
	            case 66:
	                return "e-Portrait";
	            case 67:
	                return "Soft Background Shot";
	            case 142:
	                return "Hand-held Starlight";
	            case 154:
	                return "HDR";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getNoiseReductionDescription()
	    {
	        Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagNoiseReduction);
	        if (value == null)
	            return null;

	        if (value == 0)
	            return "(none)";

	        StringBuilder sb = new StringBuilder();
	        int v = value;

	        if ((v & 1) != 0) sb.append("Noise Reduction, ");
	        if (((v >> 1) & 1) != 0) sb.append("Noise Filter, ");
	        if (((v >> 2) & 1) != 0) sb.append("Noise Filter (ISO Boost), ");
	        if (((v >> 3) & 1) != 0) sb.append("Auto, ");

	        return sb.length() != 0
	            ? sb.substring(0, sb.length() - 2)
	            : "(none)";
	    }

	    @Nullable
	    public String getDistortionCorrectionDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagDistortionCorrection, "Off", "On");
	    }

	    @Nullable
	    public String getShadingCompensationDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagShadingCompensation, "Off", "On");
	    }

	    /** 3 or 4 values */
	    @Nullable
	    public String getGradationDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagGradation);
	        if (values == null || values.length < 3)
	            return null;

	        String join = String.format("%d %d %d", values[0], values[1], values[2]);

	        String ret;
	        if (join.equals("0 0 0")) {
	            ret = "n/a";
	        } else if (join.equals("-1 -1 1")) {
	            ret = "Low Key";
	        } else if (join.equals("0 -1 1")) {
	            ret = "Normal";
	        } else if (join.equals("1 -1 1")) {
	            ret = "High Key";
	        } else {
	            ret = "Unknown (" + join + ")";
	        }

	        if (values.length > 3) {
	            if (values[3] == 0)
	                ret += "; User-Selected";
	            else if (values[3] == 1)
	                ret += "; Auto-Override";
	        }

	        return ret;
	    }

	    /** 1 or 2 values */
	    @Nullable
	    public String getPictureModeDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagPictureMode);
	        if (values == null) {
	            // check if it's only one value long also
	            Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagNoiseReduction);
	            if (value == null)
	                return null;

	            values = new int[]{value};
	        }

	        if (values.length == 0)
	            return null;

	        StringBuilder sb = new StringBuilder();
	        switch (values[0]) {
	            case 1:
	                sb.append("Vivid");
	                break;
	            case 2:
	                sb.append("Natural");
	                break;
	            case 3:
	                sb.append("Muted");
	                break;
	            case 4:
	                sb.append("Portrait");
	                break;
	            case 5:
	                sb.append("i-Enhance");
	                break;
	            case 256:
	                sb.append("Monotone");
	                break;
	            case 512:
	                sb.append("Sepia");
	                break;
	            default:
	                sb.append("Unknown (").append(values[0]).append(")");
	                break;
	        }

	        if (values.length > 1)
	            sb.append("; ").append(values[1]);

	        return sb.toString();
	    }

	    @Nullable
	    public String getPictureModeSaturationDescription()
	    {
	        return getValueMinMaxDescription(OlympusCameraSettingsMakernoteDirectory.TagPictureModeSaturation);
	    }

	    @Nullable
	    public String getPictureModeContrastDescription()
	    {
	        return getValueMinMaxDescription(OlympusCameraSettingsMakernoteDirectory.TagPictureModeContrast);
	    }

	    @Nullable
	    public String getPictureModeSharpnessDescription()
	    {
	        return getValueMinMaxDescription(OlympusCameraSettingsMakernoteDirectory.TagPictureModeSharpness);
	    }

	    @Nullable
	    public String getPictureModeBWFilterDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagPictureModeBWFilter,
	            "n/a", "Neutral", "Yellow", "Orange", "Red", "Green");
	    }

	    @Nullable
	    public String getPictureModeToneDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagPictureModeTone,
	            "n/a", "Neutral", "Sepia", "Blue", "Purple", "Green");
	    }

	    @Nullable
	    public String getNoiseFilterDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagNoiseFilter);
	        if (values == null)
	            return null;

	        String join = String.format("%d %d %d", values[0], values[1], values[2]);

	        if (join.equals("0 0 0"))
	            return "n/a";
	        if (join.equals("-2 -2 1"))
	            return "Off";
	        if (join.equals("-1 -2 1"))
	            return "Low";
	        if (join.equals("0 -2 1"))
	            return "Standard";
	        if (join.equals("1 -2 1"))
	            return "High";
	        return "Unknown (" + join + ")";
	    }

	    @Nullable
	    public String getArtFilterDescription()
	    {
	        return getFiltersDescription(OlympusCameraSettingsMakernoteDirectory.TagArtFilter);
	    }

	    @Nullable
	    public String getMagicFilterDescription()
	    {
	        return getFiltersDescription(OlympusCameraSettingsMakernoteDirectory.TagMagicFilter);
	    }

	    @Nullable
	    public String getPictureModeEffectDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagPictureModeEffect);
	        if (values == null)
	            return null;

	        String key = String.format("%d %d %d", values[0], values[1], values[2]);
	        if (key.equals("0 0 0"))
	            return "n/a";
	        if (key.equals("-1 -1 1"))
	            return "Low";
	        if (key.equals("0 -1 1"))
	            return "Standard";
	        if (key.equals("1 -1 1"))
	            return "High";
	        return "Unknown (" + key + ")";
	    }

	    @Nullable
	    public String getToneLevelDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagToneLevel);
	        if (values == null || values.length == 0)
	            return null;

	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < values.length; i++) {
	            if (i == 0 || i == 4 || i == 8 || i == 12 || i == 16 || i == 20 || i == 24)
	                sb.append(_toneLevelType.get(values[i])).append("; ");
	            else
	                sb.append(values[i]).append("; ");
	        }

	        return sb.substring(0, sb.length() - 2);
	    }

	    @Nullable
	    public String getArtFilterEffectDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagArtFilterEffect);
	        if (values == null)
	            return null;

	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < values.length; i++) {
	            if (i == 0) {
	                sb.append((_filters.containsKey(values[i]) ? _filters.get(values[i]) : "[unknown]")).append("; ");
	            } else if (i == 3) {
	                sb.append("Partial Color ").append(values[i]).append("; ");
	            } else if (i == 4) {
	                switch (values[i]) {
	                    case 0x0000:
	                        sb.append("No Effect");
	                        break;
	                    case 0x8010:
	                        sb.append("Star Light");
	                        break;
	                    case 0x8020:
	                        sb.append("Pin Hole");
	                        break;
	                    case 0x8030:
	                        sb.append("Frame");
	                        break;
	                    case 0x8040:
	                        sb.append("Soft Focus");
	                        break;
	                    case 0x8050:
	                        sb.append("White Edge");
	                        break;
	                    case 0x8060:
	                        sb.append("B&W");
	                        break;
	                    default:
	                        sb.append("Unknown (").append(values[i]).append(")");
	                        break;
	                }
	                sb.append("; ");
	            } else if (i == 6) {
	                switch (values[i]) {
	                    case 0:
	                        sb.append("No Color Filter");
	                        break;
	                    case 1:
	                        sb.append("Yellow Color Filter");
	                        break;
	                    case 2:
	                        sb.append("Orange Color Filter");
	                        break;
	                    case 3:
	                        sb.append("Red Color Filter");
	                        break;
	                    case 4:
	                        sb.append("Green Color Filter");
	                        break;
	                    default:
	                        sb.append("Unknown (").append(values[i]).append(")");
	                        break;
	                }
	                sb.append("; ");
	            } else {
	                sb.append(values[i]).append("; ");
	            }
	        }

	        return sb.substring(0, sb.length() - 2);
	    }

	    @Nullable
	    public String getColorCreatorEffectDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagColorCreatorEffect);
	        if (values == null)
	            return null;

	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < values.length; i++) {
	            if (i == 0) {
	                sb.append("Color ").append(values[i]).append("; ");
	            } else if (i == 3) {
	                sb.append("Strength ").append(values[i]).append("; ");
	            } else {
	                sb.append(values[i]).append("; ");
	            }
	        }

	        return sb.substring(0, sb.length() - 2);
	    }

	    /** 2 or 3 numbers: 1. Mode, 2. Shot number, 3. Mode bits */
	    @Nullable
	    public String getDriveModeDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagDriveMode);
	        if (values == null)
	            return null;

	        if (values.length == 0 || values[0] == 0)
	            return "Single Shot";

	        StringBuilder a = new StringBuilder();

	        if (values[0] == 5 && values.length >= 3) {
	            int c = values[2];
	            if (( c       & 1) > 0) a.append("AE");
	            if (((c >> 1) & 1) > 0) a.append("WB");
	            if (((c >> 2) & 1) > 0) a.append("FL");
	            if (((c >> 3) & 1) > 0) a.append("MF");
	            if (((c >> 6) & 1) > 0) a.append("Focus");

	            a.append(" Bracketing");
	        } else {
	            switch (values[0]) {
	                case 1:
	                    a.append("Continuous Shooting");
	                    break;
	                case 2:
	                    a.append("Exposure Bracketing");
	                    break;
	                case 3:
	                    a.append("White Balance Bracketing");
	                    break;
	                case 4:
	                    a.append("Exposure+WB Bracketing");
	                    break;
	                default:
	                    a.append("Unknown (").append(values[0]).append(")");
	                    break;
	            }
	        }

	        a.append(", Shot ").append(values[1]);

	        return a.toString();
	    }

	    /** 2 numbers: 1. Mode, 2. Shot number */
	    @Nullable
	    public String getPanoramaModeDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagPanoramaMode);
	        if (values == null)
	            return null;

	        if (values.length == 0 || values[0] == 0)
	            return "Off";

	        String a;
	        switch (values[0]) {
	            case 1:
	                a = "Left to Right";
	                break;
	            case 2:
	                a = "Right to Left";
	                break;
	            case 3:
	                a = "Bottom to Top";
	                break;
	            case 4:
	                a = "Top to Bottom";
	                break;
	            default:
	                a = "Unknown (" + values[0] + ")";
	                break;
	        }

	        return String.format("%s, Shot %d", a, values[1]);
	    }

	    @Nullable
	    public String getImageQuality2Description()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagImageQuality2, 1,
	            "SQ", "HQ", "SHQ", "RAW", "SQ (5)");
	    }

	    @Nullable
	    public String getImageStabilizationDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagImageStabilization,
	            "Off", "On, Mode 1", "On, Mode 2", "On, Mode 3", "On, Mode 4");
	    }

	    @Nullable
	    public String getStackedImageDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagStackedImage);
	        if (values == null || values.length < 2)
	            return null;

	        int v1 = values[0];
	        int v2 = values[1];

	        if (v1 == 0 && v2 == 0)
	            return "No";
	        if (v1 == 9 && v2 == 8)
	            return "Focus-stacked (8 images)";

	        return String.format("Unknown (%d %d)", v1, v2);
	    }

	    /// <remarks>
	    /// TODO: need better image examples to test this function
	    /// </remarks>
	    /// <returns></returns>
	    @Nullable
	    public String getManometerPressureDescription()
	    {
	        Integer value = _directory.getInteger(OlympusCameraSettingsMakernoteDirectory.TagManometerPressure);
	        if (value == null)
	            return null;

	        return String.format("%s kPa", new DecimalFormat("#.##").format(value / 10.0));
	    }

	    /// <remarks>
	    /// TODO: need better image examples to test this function
	    /// </remarks>
	    /// <returns></returns>
	    @Nullable
	    public String getManometerReadingDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagManometerReading);
	        if (values == null || values.length < 2)
	            return null;

	        DecimalFormat format = new DecimalFormat("#.##");
	        return String.format("%s m, %s ft",
	            format.format(values[0] / 10.0),
	            format.format(values[1] / 10.0));
	    }

	    @Nullable
	    public String getExtendedWBDetectDescription()
	    {
	        return getIndexedDescription(OlympusCameraSettingsMakernoteDirectory.TagExtendedWBDetect, "Off", "On");
	    }

	    /** converted to degrees of clockwise camera rotation */
	    // TODO: need better image examples to test this function
	    @Nullable
	    public String getRollAngleDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagRollAngle);
	        if (values == null || values.length < 2)
	            return null;

	        String ret = values[0] != 0
	            ? Double.toString(-values[0] / 10.0)
	            : "n/a";

	        return String.format("%s %d", ret, values[1]);
	    }

	    /** converted to degrees of upward camera tilt */
	    // TODO: need better image examples to test this function
	    @Nullable
	    public String getPitchAngleDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusCameraSettingsMakernoteDirectory.TagPitchAngle);
	        if (values == null || values.length < 2)
	            return null;

	        // (second value is 0 if level gauge is off)
	        String ret = values[0] != 0
	            ? Double.toString(values[0] / 10.0)
	            : "n/a";

	        return String.format("%s %d", ret, values[1]);
	    }

	    @Nullable
	    public String getDateTimeUTCDescription()
	    {
	        Object value = _directory.getObject(OlympusCameraSettingsMakernoteDirectory.TagDateTimeUtc);
	        if (value == null)
	            return null;
	        return value.toString();
	    }

	    @Nullable
	    private String getValueMinMaxDescription(int tagId)
	    {
	        int[] values = _directory.getIntArray(tagId);
	        if (values == null || values.length < 3)
	            return null;

	        return String.format("%d (min %d, max %d)", values[0], values[1], values[2]);
	    }

	    @Nullable
	    private String getFiltersDescription(int tagId)
	    {
	        int[] values = _directory.getIntArray(tagId);
	        if (values == null || values.length == 0)
	            return null;

	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < values.length; i++) {
	            if (i == 0)
	                sb.append(_filters.containsKey(values[i]) ? _filters.get(values[i]) : "[unknown]");
	            else
	                sb.append(values[i]);
	            sb.append("; ");
	        }

	        return sb.substring(0, sb.length() - 2);
	    }

	    private final HashMap<Integer, String> _toneLevelType = new HashMap<Integer, String>();
	    // ArtFilter, ArtFilterEffect and MagicFilter values
	    private final HashMap<Integer, String> _filters = new HashMap<Integer, String>();

	     {
	        _filters.put(0, "Off");
	        _filters.put(1, "Soft Focus");
	        _filters.put(2, "Pop Art");
	        _filters.put(3, "Pale & Light Color");
	        _filters.put(4, "Light Tone");
	        _filters.put(5, "Pin Hole");
	        _filters.put(6, "Grainy Film");
	        _filters.put(9, "Diorama");
	        _filters.put(10, "Cross Process");
	        _filters.put(12, "Fish Eye");
	        _filters.put(13, "Drawing");
	        _filters.put(14, "Gentle Sepia");
	        _filters.put(15, "Pale & Light Color II");
	        _filters.put(16, "Pop Art II");
	        _filters.put(17, "Pin Hole II");
	        _filters.put(18, "Pin Hole III");
	        _filters.put(19, "Grainy Film II");
	        _filters.put(20, "Dramatic Tone");
	        _filters.put(21, "Punk");
	        _filters.put(22, "Soft Focus 2");
	        _filters.put(23, "Sparkle");
	        _filters.put(24, "Watercolor");
	        _filters.put(25, "Key Line");
	        _filters.put(26, "Key Line II");
	        _filters.put(27, "Miniature");
	        _filters.put(28, "Reflection");
	        _filters.put(29, "Fragmented");
	        _filters.put(31, "Cross Process II");
	        _filters.put(32, "Dramatic Tone II");
	        _filters.put(33, "Watercolor I");
	        _filters.put(34, "Watercolor II");
	        _filters.put(35, "Diorama II");
	        _filters.put(36, "Vintage");
	        _filters.put(37, "Vintage II");
	        _filters.put(38, "Vintage III");
	        _filters.put(39, "Partial Color");
	        _filters.put(40, "Partial Color II");
	        _filters.put(41, "Partial Color III");

	        _toneLevelType.put(0, "0");
	        _toneLevelType.put(-31999, "Highlights ");
	        _toneLevelType.put(-31998, "Shadows ");
	        _toneLevelType.put(-31997, "Midtones ");
	    }
	}

	
	/**
	 * The Olympus camera settings makernote is used by many manufacturers (Epson, Konica, Minolta and Agfa...), and as such contains some tags
	 * that appear specific to those manufacturers.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusCameraSettingsMakernoteDirectory extends Directory
	{
	    public static final int TagCameraSettingsVersion = 0x0000;
	    public static final int TagPreviewImageValid = 0x0100;
	    public static final int TagPreviewImageStart = 0x0101;
	    public static final int TagPreviewImageLength = 0x0102;

	    public static final int TagExposureMode = 0x0200;
	    public static final int TagAeLock = 0x0201;
	    public static final int TagMeteringMode = 0x0202;
	    public static final int TagExposureShift = 0x0203;
	    public static final int TagNdFilter = 0x0204;

	    public static final int TagMacroMode = 0x0300;
	    public static final int TagFocusMode = 0x0301;
	    public static final int TagFocusProcess = 0x0302;
	    public static final int TagAfSearch = 0x0303;
	    public static final int TagAfAreas = 0x0304;
	    public static final int TagAfPointSelected = 0x0305;
	    public static final int TagAfFineTune = 0x0306;
	    public static final int TagAfFineTuneAdj = 0x0307;

	    public static final int TagFlashMode = 0x400;
	    public static final int TagFlashExposureComp = 0x401;
	    public static final int TagFlashRemoteControl = 0x403;
	    public static final int TagFlashControlMode = 0x404;
	    public static final int TagFlashIntensity = 0x405;
	    public static final int TagManualFlashStrength = 0x406;

	    public static final int TagWhiteBalance2 = 0x500;
	    public static final int TagWhiteBalanceTemperature = 0x501;
	    public static final int TagWhiteBalanceBracket = 0x502;
	    public static final int TagCustomSaturation = 0x503;
	    public static final int TagModifiedSaturation = 0x504;
	    public static final int TagContrastSetting = 0x505;
	    public static final int TagSharpnessSetting = 0x506;
	    public static final int TagColorSpace = 0x507;
	    public static final int TagSceneMode = 0x509;
	    public static final int TagNoiseReduction = 0x50a;
	    public static final int TagDistortionCorrection = 0x50b;
	    public static final int TagShadingCompensation = 0x50c;
	    public static final int TagCompressionFactor = 0x50d;
	    public static final int TagGradation = 0x50f;
	    public static final int TagPictureMode = 0x520;
	    public static final int TagPictureModeSaturation = 0x521;
	    public static final int TagPictureModeHue = 0x522;
	    public static final int TagPictureModeContrast = 0x523;
	    public static final int TagPictureModeSharpness = 0x524;
	    public static final int TagPictureModeBWFilter = 0x525;
	    public static final int TagPictureModeTone = 0x526;
	    public static final int TagNoiseFilter = 0x527;
	    public static final int TagArtFilter = 0x529;
	    public static final int TagMagicFilter = 0x52c;
	    public static final int TagPictureModeEffect = 0x52d;
	    public static final int TagToneLevel = 0x52e;
	    public static final int TagArtFilterEffect = 0x52f;
	    public static final int TagColorCreatorEffect = 0x532;

	    public static final int TagDriveMode = 0x600;
	    public static final int TagPanoramaMode = 0x601;
	    public static final int TagImageQuality2 = 0x603;
	    public static final int TagImageStabilization = 0x604;

	    public static final int TagStackedImage = 0x804;

	    public static final int TagManometerPressure = 0x900;
	    public static final int TagManometerReading = 0x901;
	    public static final int TagExtendedWBDetect = 0x902;
	    public static final int TagRollAngle = 0x903;
	    public static final int TagPitchAngle = 0x904;
	    public static final int TagDateTimeUtc = 0x908;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TagCameraSettingsVersion, "Camera Settings Version");
	        _tagNameMap.put(TagPreviewImageValid, "Preview Image Valid");
	        _tagNameMap.put(TagPreviewImageStart, "Preview Image Start");
	        _tagNameMap.put(TagPreviewImageLength, "Preview Image Length");

	        _tagNameMap.put(TagExposureMode, "Exposure Mode");
	        _tagNameMap.put(TagAeLock, "AE Lock");
	        _tagNameMap.put(TagMeteringMode, "Metering Mode");
	        _tagNameMap.put(TagExposureShift, "Exposure Shift");
	        _tagNameMap.put(TagNdFilter, "ND Filter");

	        _tagNameMap.put(TagMacroMode, "Macro Mode");
	        _tagNameMap.put(TagFocusMode, "Focus Mode");
	        _tagNameMap.put(TagFocusProcess, "Focus Process");
	        _tagNameMap.put(TagAfSearch, "AF Search");
	        _tagNameMap.put(TagAfAreas, "AF Areas");
	        _tagNameMap.put(TagAfPointSelected, "AF Point Selected");
	        _tagNameMap.put(TagAfFineTune, "AF Fine Tune");
	        _tagNameMap.put(TagAfFineTuneAdj, "AF Fine Tune Adj");

	        _tagNameMap.put(TagFlashMode, "Flash Mode");
	        _tagNameMap.put(TagFlashExposureComp, "Flash Exposure Comp");
	        _tagNameMap.put(TagFlashRemoteControl, "Flash Remote Control");
	        _tagNameMap.put(TagFlashControlMode, "Flash Control Mode");
	        _tagNameMap.put(TagFlashIntensity, "Flash Intensity");
	        _tagNameMap.put(TagManualFlashStrength, "Manual Flash Strength");

	        _tagNameMap.put(TagWhiteBalance2, "White Balance 2");
	        _tagNameMap.put(TagWhiteBalanceTemperature, "White Balance Temperature");
	        _tagNameMap.put(TagWhiteBalanceBracket, "White Balance Bracket");
	        _tagNameMap.put(TagCustomSaturation, "Custom Saturation");
	        _tagNameMap.put(TagModifiedSaturation, "Modified Saturation");
	        _tagNameMap.put(TagContrastSetting, "Contrast Setting");
	        _tagNameMap.put(TagSharpnessSetting, "Sharpness Setting");
	        _tagNameMap.put(TagColorSpace, "Color Space");
	        _tagNameMap.put(TagSceneMode, "Scene Mode");
	        _tagNameMap.put(TagNoiseReduction, "Noise Reduction");
	        _tagNameMap.put(TagDistortionCorrection, "Distortion Correction");
	        _tagNameMap.put(TagShadingCompensation, "Shading Compensation");
	        _tagNameMap.put(TagCompressionFactor, "Compression Factor");
	        _tagNameMap.put(TagGradation, "Gradation");
	        _tagNameMap.put(TagPictureMode, "Picture Mode");
	        _tagNameMap.put(TagPictureModeSaturation, "Picture Mode Saturation");
	        _tagNameMap.put(TagPictureModeHue, "Picture Mode Hue");
	        _tagNameMap.put(TagPictureModeContrast, "Picture Mode Contrast");
	        _tagNameMap.put(TagPictureModeSharpness, "Picture Mode Sharpness");
	        _tagNameMap.put(TagPictureModeBWFilter, "Picture Mode BW Filter");
	        _tagNameMap.put(TagPictureModeTone, "Picture Mode Tone");
	        _tagNameMap.put(TagNoiseFilter, "Noise Filter");
	        _tagNameMap.put(TagArtFilter, "Art Filter");
	        _tagNameMap.put(TagMagicFilter, "Magic Filter");
	        _tagNameMap.put(TagPictureModeEffect, "Picture Mode Effect");
	        _tagNameMap.put(TagToneLevel, "Tone Level");
	        _tagNameMap.put(TagArtFilterEffect, "Art Filter Effect");
	        _tagNameMap.put(TagColorCreatorEffect, "Color Creator Effect");

	        _tagNameMap.put(TagDriveMode, "Drive Mode");
	        _tagNameMap.put(TagPanoramaMode, "Panorama Mode");
	        _tagNameMap.put(TagImageQuality2, "Image Quality 2");
	        _tagNameMap.put(TagImageStabilization, "Image Stabilization");

	        _tagNameMap.put(TagStackedImage, "Stacked Image");

	        _tagNameMap.put(TagManometerPressure, "Manometer Pressure");
	        _tagNameMap.put(TagManometerReading, "Manometer Reading");
	        _tagNameMap.put(TagExtendedWBDetect, "Extended WB Detect");
	        _tagNameMap.put(TagRollAngle, "Roll Angle");
	        _tagNameMap.put(TagPitchAngle, "Pitch Angle");
	        _tagNameMap.put(TagDateTimeUtc, "Date Time UTC");
	    }

	    public OlympusCameraSettingsMakernoteDirectory()
	    {
	        this.setDescriptor(new OlympusCameraSettingsMakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Olympus Camera Settings";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable String representations of tag values stored in a {@link OlympusRawDevelopmentMakernoteDirectory}.
	 * <p>
	 * Some Description functions converted from Exiftool version 10.10 created by Phil Harvey
	 * http://www.sno.phy.queensu.ca/~phil/exiftool/
	 * lib\Image\ExifTool\Olympus.pm
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusRawDevelopmentMakernoteDescriptor extends TagDescriptor<OlympusRawDevelopmentMakernoteDirectory>
	{
	    public OlympusRawDevelopmentMakernoteDescriptor(@NotNull OlympusRawDevelopmentMakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case OlympusRawDevelopmentMakernoteDirectory.TagRawDevVersion:
	                return getRawDevVersionDescription();
	            case OlympusRawDevelopmentMakernoteDirectory.TagRawDevColorSpace:
	                return getRawDevColorSpaceDescription();
	            case OlympusRawDevelopmentMakernoteDirectory.TagRawDevEngine:
	                return getRawDevEngineDescription();
	            case OlympusRawDevelopmentMakernoteDirectory.TagRawDevNoiseReduction:
	                return getRawDevNoiseReductionDescription();
	            case OlympusRawDevelopmentMakernoteDirectory.TagRawDevEditStatus:
	                return getRawDevEditStatusDescription();
	            case OlympusRawDevelopmentMakernoteDirectory.TagRawDevSettings:
	                return getRawDevSettingsDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getRawDevVersionDescription()
	    {
	        return getVersionBytesDescription(OlympusRawDevelopmentMakernoteDirectory.TagRawDevVersion, 4);
	    }

	    @Nullable
	    public String getRawDevColorSpaceDescription()
	    {
	        return getIndexedDescription(OlympusRawDevelopmentMakernoteDirectory.TagRawDevColorSpace,
	            "sRGB", "Adobe RGB", "Pro Photo RGB");
	    }

	    @Nullable
	    public String getRawDevEngineDescription()
	    {
	        return getIndexedDescription(OlympusRawDevelopmentMakernoteDirectory.TagRawDevEngine,
	            "High Speed", "High Function", "Advanced High Speed", "Advanced High Function");
	    }

	    @Nullable
	    public String getRawDevNoiseReductionDescription()
	    {
	        Integer value = _directory.getInteger(OlympusRawDevelopmentMakernoteDirectory.TagRawDevNoiseReduction);
	        if (value == null)
	            return null;

	        if (value == 0)
	            return "(none)";

	        StringBuilder sb = new StringBuilder();
	        int v = value;

	        if ((v        & 1) != 0) sb.append("Noise Reduction, ");
	        if (((v >> 1) & 1) != 0) sb.append("Noise Filter, ");
	        if (((v >> 2) & 1) != 0) sb.append("Noise Filter (ISO Boost), ");

	        return sb.substring(0, sb.length() - 2);
	    }

	    @Nullable
	    public String getRawDevEditStatusDescription()
	    {
	        Integer value = _directory.getInteger(OlympusRawDevelopmentMakernoteDirectory.TagRawDevEditStatus);
	        if (value == null)
	            return null;

	        switch (value)
	        {
	            case 0:
	                return "Original";
	            case 1:
	                return "Edited (Landscape)";
	            case 6:
	            case 8:
	                return "Edited (Portrait)";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getRawDevSettingsDescription()
	    {
	        Integer value = _directory.getInteger(OlympusRawDevelopmentMakernoteDirectory.TagRawDevSettings);
	        if (value == null)
	            return null;

	        if (value == 0)
	            return "(none)";

	        StringBuilder sb = new StringBuilder();
	        int v = value;

	        if ((v        & 1) != 0) sb.append("WB Color Temp, ");
	        if (((v >> 1) & 1) != 0) sb.append("WB Gray Point, ");
	        if (((v >> 2) & 1) != 0) sb.append("Saturation, ");
	        if (((v >> 3) & 1) != 0) sb.append("Contrast, ");
	        if (((v >> 4) & 1) != 0) sb.append("Sharpness, ");
	        if (((v >> 5) & 1) != 0) sb.append("Color Space, ");
	        if (((v >> 6) & 1) != 0) sb.append("High Function, ");
	        if (((v >> 7) & 1) != 0) sb.append("Noise Reduction, ");

	        return sb.substring(0, sb.length() - 2);
	    }

	}


	/**
	 * The Olympus raw development makernote is used by many manufacturers (Epson, Konica, Minolta and Agfa...), and as such contains some tags
	 * that appear specific to those manufacturers.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusRawDevelopmentMakernoteDirectory extends Directory
	{
	    public static final int TagRawDevVersion = 0x0000;
	    public static final int TagRawDevExposureBiasValue = 0x0100;
	    public static final int TagRawDevWhiteBalanceValue = 0x0101;
	    public static final int TagRawDevWbFineAdjustment = 0x0102;
	    public static final int TagRawDevGrayPoint = 0x0103;
	    public static final int TagRawDevSaturationEmphasis = 0x0104;
	    public static final int TagRawDevMemoryColorEmphasis = 0x0105;
	    public static final int TagRawDevContrastValue = 0x0106;
	    public static final int TagRawDevSharpnessValue = 0x0107;
	    public static final int TagRawDevColorSpace = 0x0108;
	    public static final int TagRawDevEngine = 0x0109;
	    public static final int TagRawDevNoiseReduction = 0x010a;
	    public static final int TagRawDevEditStatus = 0x010b;
	    public static final int TagRawDevSettings = 0x010c;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TagRawDevVersion, "Raw Dev Version");
	        _tagNameMap.put(TagRawDevExposureBiasValue, "Raw Dev Exposure Bias Value");
	        _tagNameMap.put(TagRawDevWhiteBalanceValue, "Raw Dev White Balance Value");
	        _tagNameMap.put(TagRawDevWbFineAdjustment, "Raw Dev WB Fine Adjustment");
	        _tagNameMap.put(TagRawDevGrayPoint, "Raw Dev Gray Point");
	        _tagNameMap.put(TagRawDevSaturationEmphasis, "Raw Dev Saturation Emphasis");
	        _tagNameMap.put(TagRawDevMemoryColorEmphasis, "Raw Dev Memory Color Emphasis");
	        _tagNameMap.put(TagRawDevContrastValue, "Raw Dev Contrast Value");
	        _tagNameMap.put(TagRawDevSharpnessValue, "Raw Dev Sharpness Value");
	        _tagNameMap.put(TagRawDevColorSpace, "Raw Dev Color Space");
	        _tagNameMap.put(TagRawDevEngine, "Raw Dev Engine");
	        _tagNameMap.put(TagRawDevNoiseReduction, "Raw Dev Noise Reduction");
	        _tagNameMap.put(TagRawDevEditStatus, "Raw Dev Edit Status");
	        _tagNameMap.put(TagRawDevSettings, "Raw Dev Settings");
	    }

	    public OlympusRawDevelopmentMakernoteDirectory()
	    {
	        this.setDescriptor(new OlympusRawDevelopmentMakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Olympus Raw Development";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable String representations of tag values stored in a {@link OlympusRawDevelopment2MakernoteDirectory}.
	 * <p>
	 * Some Description functions converted from Exiftool version 10.10 created by Phil Harvey
	 * http://www.sno.phy.queensu.ca/~phil/exiftool/
	 * lib\Image\ExifTool\Olympus.pm
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusRawDevelopment2MakernoteDescriptor extends TagDescriptor<OlympusRawDevelopment2MakernoteDirectory>
	{
	    public OlympusRawDevelopment2MakernoteDescriptor(@NotNull OlympusRawDevelopment2MakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case OlympusRawDevelopment2MakernoteDirectory.TagRawDevVersion:
	                return getRawDevVersionDescription();
	            case OlympusRawDevelopment2MakernoteDirectory.TagRawDevExposureBiasValue:
	                return getRawDevExposureBiasValueDescription();
	            case OlympusRawDevelopment2MakernoteDirectory.TagRawDevColorSpace:
	                return getRawDevColorSpaceDescription();
	            case OlympusRawDevelopment2MakernoteDirectory.TagRawDevNoiseReduction:
	                return getRawDevNoiseReductionDescription();
	            case OlympusRawDevelopment2MakernoteDirectory.TagRawDevEngine:
	                return getRawDevEngineDescription();
	            case OlympusRawDevelopment2MakernoteDirectory.TagRawDevPictureMode:
	                return getRawDevPictureModeDescription();
	            case OlympusRawDevelopment2MakernoteDirectory.TagRawDevPmBwFilter:
	                return getRawDevPmBwFilterDescription();
	            case OlympusRawDevelopment2MakernoteDirectory.TagRawDevPmPictureTone:
	                return getRawDevPmPictureToneDescription();
	            case OlympusRawDevelopment2MakernoteDirectory.TagRawDevArtFilter:
	                return getRawDevArtFilterDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getRawDevVersionDescription()
	    {
	        return getVersionBytesDescription(OlympusRawDevelopment2MakernoteDirectory.TagRawDevVersion, 4);
	    }

	    @Nullable
	    public String getRawDevExposureBiasValueDescription()
	    {
	        return getIndexedDescription(OlympusRawDevelopment2MakernoteDirectory.TagRawDevExposureBiasValue,
	                1, "Color Temperature", "Gray Point");
	    }

	    @Nullable
	    public String getRawDevColorSpaceDescription()
	    {
	        return getIndexedDescription(OlympusRawDevelopment2MakernoteDirectory.TagRawDevColorSpace,
	            "sRGB", "Adobe RGB", "Pro Photo RGB");
	    }

	    @Nullable
	    public String getRawDevNoiseReductionDescription()
	    {
	        Integer value = _directory.getInteger(OlympusRawDevelopment2MakernoteDirectory.TagRawDevNoiseReduction);
	        if (value == null)
	            return null;

	        if (value == 0)
	            return "(none)";

	        StringBuilder sb = new StringBuilder();
	        int v = value;

	        if ((v        & 1) != 0) sb.append("Noise Reduction, ");
	        if (((v >> 1) & 1) != 0) sb.append("Noise Filter, ");
	        if (((v >> 2) & 1) != 0) sb.append("Noise Filter (ISO Boost), ");
	        if (((v >> 3) & 1) != 0) sb.append("Noise Filter (Auto), ");
	        
	        if (sb.length() > 2) {
	            sb.delete(sb.length() - 2, sb.length());
	        }
	        return sb.toString();
	    }

	    @Nullable
	    public String getRawDevEngineDescription()
	    {
	        return getIndexedDescription(OlympusRawDevelopment2MakernoteDirectory.TagRawDevEngine,
	            "High Speed", "High Function", "Advanced High Speed", "Advanced High Function");
	    }

	    @Nullable
	    public String getRawDevPictureModeDescription()
	    {
	        Integer value = _directory.getInteger(OlympusRawDevelopment2MakernoteDirectory.TagRawDevPictureMode);
	        if (value == null)
	            return null;

	        switch (value)
	        {
	            case 1:
	                return "Vivid";
	            case 2:
	                return "Natural";
	            case 3:
	                return "Muted";
	            case 256:
	                return "Monotone";
	            case 512:
	                return "Sepia";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getRawDevPmBwFilterDescription()
	    {
	        return getIndexedDescription(OlympusRawDevelopment2MakernoteDirectory.TagRawDevPmBwFilter,
	            "Neutral", "Yellow", "Orange", "Red", "Green");
	    }

	    @Nullable
	    public String getRawDevPmPictureToneDescription()
	    {
	        return getIndexedDescription(OlympusRawDevelopment2MakernoteDirectory.TagRawDevPmPictureTone,
	            "Neutral", "Sepia", "Blue", "Purple", "Green");
	    }

	    @Nullable
	    public String getRawDevArtFilterDescription()
	    {
	        return getFilterDescription(OlympusRawDevelopment2MakernoteDirectory.TagRawDevArtFilter);
	    }

	    @Nullable
	    public String getFilterDescription(int tag)
	    {
	        int[] values = _directory.getIntArray(tag);
	        if (values == null || values.length == 0)
	            return null;

	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < values.length; i++) {
	            if (i == 0)
	                sb.append(_filters.containsKey(values[i]) ? _filters.get(values[i]) : "[unknown]");
	            else
	                sb.append(values[i]).append("; ");
	            sb.append("; ");
	        }

	        return sb.substring(0, sb.length() - 2);
	    }

	    // RawDevArtFilter values
	    private final HashMap<Integer, String> _filters = new HashMap<Integer, String>();

	     {
	        _filters.put(0, "Off");
	        _filters.put(1, "Soft Focus");
	        _filters.put(2, "Pop Art");
	        _filters.put(3, "Pale & Light Color");
	        _filters.put(4, "Light Tone");
	        _filters.put(5, "Pin Hole");
	        _filters.put(6, "Grainy Film");
	        _filters.put(9, "Diorama");
	        _filters.put(10, "Cross Process");
	        _filters.put(12, "Fish Eye");
	        _filters.put(13, "Drawing");
	        _filters.put(14, "Gentle Sepia");
	        _filters.put(15, "Pale & Light Color II");
	        _filters.put(16, "Pop Art II");
	        _filters.put(17, "Pin Hole II");
	        _filters.put(18, "Pin Hole III");
	        _filters.put(19, "Grainy Film II");
	        _filters.put(20, "Dramatic Tone");
	        _filters.put(21, "Punk");
	        _filters.put(22, "Soft Focus 2");
	        _filters.put(23, "Sparkle");
	        _filters.put(24, "Watercolor");
	        _filters.put(25, "Key Line");
	        _filters.put(26, "Key Line II");
	        _filters.put(27, "Miniature");
	        _filters.put(28, "Reflection");
	        _filters.put(29, "Fragmented");
	        _filters.put(31, "Cross Process II");
	        _filters.put(32, "Dramatic Tone II");
	        _filters.put(33, "Watercolor I");
	        _filters.put(34, "Watercolor II");
	        _filters.put(35, "Diorama II");
	        _filters.put(36, "Vintage");
	        _filters.put(37, "Vintage II");
	        _filters.put(38, "Vintage III");
	        _filters.put(39, "Partial Color");
	        _filters.put(40, "Partial Color II");
	        _filters.put(41, "Partial Color III");
	    }
	}


	/**
	 * The Olympus raw development 2 makernote is used by many manufacturers (Epson, Konica, Minolta and Agfa...), and as such contains some tags
	 * that appear specific to those manufacturers.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusRawDevelopment2MakernoteDirectory extends Directory
	{
	    public static final int TagRawDevVersion = 0x0000;
	    public static final int TagRawDevExposureBiasValue = 0x0100;
	    public static final int TagRawDevWhiteBalance = 0x0101;
	    public static final int TagRawDevWhiteBalanceValue = 0x0102;
	    public static final int TagRawDevWbFineAdjustment = 0x0103;
	    public static final int TagRawDevGrayPoint = 0x0104;
	    public static final int TagRawDevContrastValue = 0x0105;
	    public static final int TagRawDevSharpnessValue = 0x0106;
	    public static final int TagRawDevSaturationEmphasis = 0x0107;
	    public static final int TagRawDevMemoryColorEmphasis = 0x0108;
	    public static final int TagRawDevColorSpace = 0x0109;
	    public static final int TagRawDevNoiseReduction = 0x010a;
	    public static final int TagRawDevEngine = 0x010b;
	    public static final int TagRawDevPictureMode = 0x010c;
	    public static final int TagRawDevPmSaturation = 0x010d;
	    public static final int TagRawDevPmContrast = 0x010e;
	    public static final int TagRawDevPmSharpness = 0x010f;
	    public static final int TagRawDevPmBwFilter = 0x0110;
	    public static final int TagRawDevPmPictureTone = 0x0111;
	    public static final int TagRawDevGradation = 0x0112;
	    public static final int TagRawDevSaturation3 = 0x0113;
	    public static final int TagRawDevAutoGradation = 0x0119;
	    public static final int TagRawDevPmNoiseFilter = 0x0120;
	    public static final int TagRawDevArtFilter = 0x0121;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TagRawDevVersion, "Raw Dev Version");
	        _tagNameMap.put(TagRawDevExposureBiasValue, "Raw Dev Exposure Bias Value");
	        _tagNameMap.put(TagRawDevWhiteBalance, "Raw Dev White Balance");
	        _tagNameMap.put(TagRawDevWhiteBalanceValue, "Raw Dev White Balance Value");
	        _tagNameMap.put(TagRawDevWbFineAdjustment, "Raw Dev WB Fine Adjustment");
	        _tagNameMap.put(TagRawDevGrayPoint, "Raw Dev Gray Point");
	        _tagNameMap.put(TagRawDevContrastValue, "Raw Dev Contrast Value");
	        _tagNameMap.put(TagRawDevSharpnessValue, "Raw Dev Sharpness Value");
	        _tagNameMap.put(TagRawDevSaturationEmphasis, "Raw Dev Saturation Emphasis");
	        _tagNameMap.put(TagRawDevMemoryColorEmphasis, "Raw Dev Memory Color Emphasis");
	        _tagNameMap.put(TagRawDevColorSpace, "Raw Dev Color Space");
	        _tagNameMap.put(TagRawDevNoiseReduction, "Raw Dev Noise Reduction");
	        _tagNameMap.put(TagRawDevEngine, "Raw Dev Engine");
	        _tagNameMap.put(TagRawDevPictureMode, "Raw Dev Picture Mode");
	        _tagNameMap.put(TagRawDevPmSaturation, "Raw Dev PM Saturation");
	        _tagNameMap.put(TagRawDevPmContrast, "Raw Dev PM Contrast");
	        _tagNameMap.put(TagRawDevPmSharpness, "Raw Dev PM Sharpness");
	        _tagNameMap.put(TagRawDevPmBwFilter, "Raw Dev PM BW Filter");
	        _tagNameMap.put(TagRawDevPmPictureTone, "Raw Dev PM Picture Tone");
	        _tagNameMap.put(TagRawDevGradation, "Raw Dev Gradation");
	        _tagNameMap.put(TagRawDevSaturation3, "Raw Dev Saturation 3");
	        _tagNameMap.put(TagRawDevAutoGradation, "Raw Dev Auto Gradation");
	        _tagNameMap.put(TagRawDevPmNoiseFilter, "Raw Dev PM Noise Filter");
	        _tagNameMap.put(TagRawDevArtFilter, "Raw Dev Art Filter");
	    }

	    public OlympusRawDevelopment2MakernoteDirectory()
	    {
	        this.setDescriptor(new OlympusRawDevelopment2MakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Olympus Raw Development 2";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable String representations of tag values stored in a {@link OlympusImageProcessingMakernoteDirectory}.
	 * <p>
	 * Some Description functions converted from Exiftool version 10.33 created by Phil Harvey
	 * http://www.sno.phy.queensu.ca/~phil/exiftool/
	 * lib\Image\ExifTool\Olympus.pm
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusImageProcessingMakernoteDescriptor extends TagDescriptor<OlympusImageProcessingMakernoteDirectory>
	{
	    public OlympusImageProcessingMakernoteDescriptor(@NotNull OlympusImageProcessingMakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case OlympusImageProcessingMakernoteDirectory.TagImageProcessingVersion:
	                return getImageProcessingVersionDescription();
	            case OlympusImageProcessingMakernoteDirectory.TagColorMatrix:
	                return getColorMatrixDescription();
	            case OlympusImageProcessingMakernoteDirectory.TagNoiseReduction2:
	                return getNoiseReduction2Description();
	            case OlympusImageProcessingMakernoteDirectory.TagDistortionCorrection2:
	                return getDistortionCorrection2Description();
	            case OlympusImageProcessingMakernoteDirectory.TagShadingCompensation2:
	                return getShadingCompensation2Description();
	            case OlympusImageProcessingMakernoteDirectory.TagMultipleExposureMode:
	                return getMultipleExposureModeDescription();
	            case OlympusImageProcessingMakernoteDirectory.TagAspectRatio:
	                return getAspectRatioDescription();
	            case OlympusImageProcessingMakernoteDirectory.TagKeystoneCompensation:
	                return getKeystoneCompensationDescription();
	            case OlympusImageProcessingMakernoteDirectory.TagKeystoneDirection:
	                return getKeystoneDirectionDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getImageProcessingVersionDescription()
	    {
	        return getVersionBytesDescription(OlympusImageProcessingMakernoteDirectory.TagImageProcessingVersion, 4);
	    }

	    @Nullable
	    public String getColorMatrixDescription()
	    {
	        int[] obj = _directory.getIntArray(OlympusImageProcessingMakernoteDirectory.TagColorMatrix);
	        if (obj == null)
	            return null;

	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < obj.length; i++) {
	            if (i != 0)
	                sb.append(" ");
	            sb.append((short)obj[i]);
	        }
	        return sb.toString();
	    }

	    @Nullable
	    public String getNoiseReduction2Description()
	    {
	        Integer value = _directory.getInteger(OlympusImageProcessingMakernoteDirectory.TagNoiseReduction2);
	        if (value == null)
	            return null;

	        if (value == 0)
	            return "(none)";

	        StringBuilder sb = new StringBuilder();
	        short v = value.shortValue();

	        if (( v       & 1) != 0) sb.append("Noise Reduction, ");
	        if (((v >> 1) & 1) != 0) sb.append("Noise Filter, ");
	        if (((v >> 2) & 1) != 0) sb.append("Noise Filter (ISO Boost), ");

	        return sb.substring(0, sb.length() - 2);
	    }

	    @Nullable
	    public String getDistortionCorrection2Description()
	    {
	        return getIndexedDescription(OlympusImageProcessingMakernoteDirectory.TagDistortionCorrection2, "Off", "On");
	    }

	    @Nullable
	    public String getShadingCompensation2Description()
	    {
	        return getIndexedDescription(OlympusImageProcessingMakernoteDirectory.TagShadingCompensation2, "Off", "On");
	    }

	    @Nullable
	    public String getMultipleExposureModeDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusImageProcessingMakernoteDirectory.TagMultipleExposureMode);
	        if (values == null)
	        {
	            // check if it's only one value long also
	            Integer value = _directory.getInteger(OlympusImageProcessingMakernoteDirectory.TagMultipleExposureMode);
	            if(value == null)
	                return null;

	            values = new int[1];
	            values[0] = value;
	        }

	        if (values.length == 0)
	            return null;

	        StringBuilder sb = new StringBuilder();

	        switch ((short)values[0])
	        {
	            case 0:
	                sb.append("Off");
	                break;
	            case 2:
	                sb.append("On (2 frames)");
	                break;
	            case 3:
	                sb.append("On (3 frames)");
	                break;
	            default:
	                sb.append("Unknown (").append((short)values[0]).append(")");
	                break;
	        }

	        if (values.length > 1)
	            sb.append("; ").append((short)values[1]);

	        return sb.toString();
	    }

	    @Nullable
	    public String getAspectRatioDescription()
	    {
	        byte[] values = _directory.getByteArray(OlympusImageProcessingMakernoteDirectory.TagAspectRatio);
	        if (values == null || values.length < 2)
	            return null;

	        String join = String.format("%d %d", values[0], values[1]);

	        String ret;
	        if(join.equals("1 1"))
	            ret = "4:3";
	        else if(join.equals("1 4"))
	            ret = "1:1";
	        else if(join.equals("2 1"))
	            ret = "3:2 (RAW)";
	        else if(join.equals("2 2"))
	            ret = "3:2";
	        else if(join.equals("3 1"))
	            ret = "16:9 (RAW)";
	        else if(join.equals("3 3"))
	            ret = "16:9";
	        else if(join.equals("4 1"))
	            ret = "1:1 (RAW)";
	        else if(join.equals("4 4"))
	            ret = "6:6";
	        else if(join.equals("5 5"))
	            ret = "5:4";
	        else if(join.equals("6 6"))
	            ret = "7:6";
	        else if(join.equals("7 7"))
	            ret = "6:5";
	        else if(join.equals("8 8"))
	            ret = "7:5";
	        else if(join.equals("9 1"))
	            ret = "3:4 (RAW)";
	        else if(join.equals("9 9"))
	            ret = "3:4";
	        else
	            ret = "Unknown (" + join + ")";

	        return ret;
	    }

	    @Nullable
	    public String getKeystoneCompensationDescription()
	    {
	        byte[] values = _directory.getByteArray(OlympusImageProcessingMakernoteDirectory.TagKeystoneCompensation);
	        if (values == null || values.length < 2)
	            return null;

	        String join = String.format("%d %d", values[0], values[1]);

	        String ret;
	        if(join.equals("0 0"))
	            ret = "Off";
	        else if(join.equals("0 1"))
	            ret = "On";
	        else
	            ret = "Unknown (" + join + ")";

	        return ret;
	    }

	    @Nullable
	    public String getKeystoneDirectionDescription()
	    {
	        return getIndexedDescription(OlympusImageProcessingMakernoteDirectory.TagKeystoneDirection, "Vertical", "Horizontal");
	    }
	}
	
	/**
	 * The Olympus image processing makernote is used by many manufacturers (Epson, Konica, Minolta and Agfa...), and as such contains some tags
	 * that appear specific to those manufacturers.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusImageProcessingMakernoteDirectory extends Directory
	{
	    public static final int TagImageProcessingVersion = 0x0000;
	    public static final int TagWbRbLevels = 0x0100;
	    // 0x0101 - in-camera AutoWB unless it is all 0's or all 256's (ref IB)
	    public static final int TagWbRbLevels3000K = 0x0102;
	    public static final int TagWbRbLevels3300K = 0x0103;
	    public static final int TagWbRbLevels3600K = 0x0104;
	    public static final int TagWbRbLevels3900K = 0x0105;
	    public static final int TagWbRbLevels4000K = 0x0106;
	    public static final int TagWbRbLevels4300K = 0x0107;
	    public static final int TagWbRbLevels4500K = 0x0108;
	    public static final int TagWbRbLevels4800K = 0x0109;
	    public static final int TagWbRbLevels5300K = 0x010a;
	    public static final int TagWbRbLevels6000K = 0x010b;
	    public static final int TagWbRbLevels6600K = 0x010c;
	    public static final int TagWbRbLevels7500K = 0x010d;
	    public static final int TagWbRbLevelsCwB1 = 0x010e;
	    public static final int TagWbRbLevelsCwB2 = 0x010f;
	    public static final int TagWbRbLevelsCwB3 = 0x0110;
	    public static final int TagWbRbLevelsCwB4 = 0x0111;
	    public static final int TagWbGLevel3000K = 0x0113;
	    public static final int TagWbGLevel3300K = 0x0114;
	    public static final int TagWbGLevel3600K = 0x0115;
	    public static final int TagWbGLevel3900K = 0x0116;
	    public static final int TagWbGLevel4000K = 0x0117;
	    public static final int TagWbGLevel4300K = 0x0118;
	    public static final int TagWbGLevel4500K = 0x0119;
	    public static final int TagWbGLevel4800K = 0x011a;
	    public static final int TagWbGLevel5300K = 0x011b;
	    public static final int TagWbGLevel6000K = 0x011c;
	    public static final int TagWbGLevel6600K = 0x011d;
	    public static final int TagWbGLevel7500K = 0x011e;
	    public static final int TagWbGLevel = 0x011f;
	    // 0x0121 = WB preset for flash (about 6000K) (ref IB)
	    // 0x0125 = WB preset for underwater (ref IB)

	    public static final int TagColorMatrix = 0x0200;
	    // color matrices (ref 11):
	    // 0x0201-0x020d are sRGB color matrices
	    // 0x020e-0x021a are Adobe RGB color matrices
	    // 0x021b-0x0227 are ProPhoto RGB color matrices
	    // 0x0228 and 0x0229 are ColorMatrix for E-330
	    // 0x0250-0x0252 are sRGB color matrices
	    // 0x0253-0x0255 are Adobe RGB color matrices
	    // 0x0256-0x0258 are ProPhoto RGB color matrices

	    public static final int TagEnhancer = 0x0300;
	    public static final int TagEnhancerValues = 0x0301;
	    public static final int TagCoringFilter = 0x0310;
	    public static final int TagCoringValues = 0x0311;
	    public static final int TagBlackLevel2 = 0x0600;
	    public static final int TagGainBase = 0x0610;
	    public static final int TagValidBits = 0x0611;
	    public static final int TagCropLeft = 0x0612;
	    public static final int TagCropTop = 0x0613;
	    public static final int TagCropWidth = 0x0614;
	    public static final int TagCropHeight = 0x0615;
	    public static final int TagUnknownBlock1 = 0x0635;
	    public static final int TagUnknownBlock2 = 0x0636;

	    // 0x0800 LensDistortionParams, float[9] (ref 11)
	    // 0x0801 LensShadingParams, int16u[16] (ref 11)
	    public static final int TagSensorCalibration = 0x0805;

	    public static final int TagNoiseReduction2 = 0x1010;
	    public static final int TagDistortionCorrection2 = 0x1011;
	    public static final int TagShadingCompensation2 = 0x1012;
	    public static final int TagMultipleExposureMode = 0x101c;
	    public static final int TagUnknownBlock3 = 0x1103;
	    public static final int TagUnknownBlock4 = 0x1104;
	    public static final int TagAspectRatio = 0x1112;
	    public static final int TagAspectFrame = 0x1113;
	    public static final int TagFacesDetected = 0x1200;
	    public static final int TagFaceDetectArea = 0x1201;
	    public static final int TagMaxFaces = 0x1202;
	    public static final int TagFaceDetectFrameSize = 0x1203;
	    public static final int TagFaceDetectFrameCrop = 0x1207;
	    public static final int TagCameraTemperature = 0x1306;

	    public static final int TagKeystoneCompensation = 0x1900;
	    public static final int TagKeystoneDirection = 0x1901;
	    // 0x1905 - focal length (PH, E-M1)
	    public static final int TagKeystoneValue = 0x1906;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TagImageProcessingVersion, "Image Processing Version");
	        _tagNameMap.put(TagWbRbLevels, "WB RB Levels");
	        _tagNameMap.put(TagWbRbLevels3000K, "WB RB Levels 3000K");
	        _tagNameMap.put(TagWbRbLevels3300K, "WB RB Levels 3300K");
	        _tagNameMap.put(TagWbRbLevels3600K, "WB RB Levels 3600K");
	        _tagNameMap.put(TagWbRbLevels3900K, "WB RB Levels 3900K");
	        _tagNameMap.put(TagWbRbLevels4000K, "WB RB Levels 4000K");
	        _tagNameMap.put(TagWbRbLevels4300K, "WB RB Levels 4300K");
	        _tagNameMap.put(TagWbRbLevels4500K, "WB RB Levels 4500K");
	        _tagNameMap.put(TagWbRbLevels4800K, "WB RB Levels 4800K");
	        _tagNameMap.put(TagWbRbLevels5300K, "WB RB Levels 5300K");
	        _tagNameMap.put(TagWbRbLevels6000K, "WB RB Levels 6000K");
	        _tagNameMap.put(TagWbRbLevels6600K, "WB RB Levels 6600K");
	        _tagNameMap.put(TagWbRbLevels7500K, "WB RB Levels 7500K");
	        _tagNameMap.put(TagWbRbLevelsCwB1, "WB RB Levels CWB1");
	        _tagNameMap.put(TagWbRbLevelsCwB2, "WB RB Levels CWB2");
	        _tagNameMap.put(TagWbRbLevelsCwB3, "WB RB Levels CWB3");
	        _tagNameMap.put(TagWbRbLevelsCwB4, "WB RB Levels CWB4");
	        _tagNameMap.put(TagWbGLevel3000K, "WB G Level 3000K");
	        _tagNameMap.put(TagWbGLevel3300K, "WB G Level 3300K");
	        _tagNameMap.put(TagWbGLevel3600K, "WB G Level 3600K");
	        _tagNameMap.put(TagWbGLevel3900K, "WB G Level 3900K");
	        _tagNameMap.put(TagWbGLevel4000K, "WB G Level 4000K");
	        _tagNameMap.put(TagWbGLevel4300K, "WB G Level 4300K");
	        _tagNameMap.put(TagWbGLevel4500K, "WB G Level 4500K");
	        _tagNameMap.put(TagWbGLevel4800K, "WB G Level 4800K");
	        _tagNameMap.put(TagWbGLevel5300K, "WB G Level 5300K");
	        _tagNameMap.put(TagWbGLevel6000K, "WB G Level 6000K");
	        _tagNameMap.put(TagWbGLevel6600K, "WB G Level 6600K");
	        _tagNameMap.put(TagWbGLevel7500K, "WB G Level 7500K");
	        _tagNameMap.put(TagWbGLevel, "WB G Level");

	        _tagNameMap.put(TagColorMatrix, "Color Matrix");

	        _tagNameMap.put(TagEnhancer, "Enhancer");
	        _tagNameMap.put(TagEnhancerValues, "Enhancer Values");
	        _tagNameMap.put(TagCoringFilter, "Coring Filter");
	        _tagNameMap.put(TagCoringValues, "Coring Values");
	        _tagNameMap.put(TagBlackLevel2, "Black Level 2");
	        _tagNameMap.put(TagGainBase, "Gain Base");
	        _tagNameMap.put(TagValidBits, "Valid Bits");
	        _tagNameMap.put(TagCropLeft, "Crop Left");
	        _tagNameMap.put(TagCropTop, "Crop Top");
	        _tagNameMap.put(TagCropWidth, "Crop Width");
	        _tagNameMap.put(TagCropHeight, "Crop Height");
	        _tagNameMap.put(TagUnknownBlock1, "Unknown Block 1");
	        _tagNameMap.put(TagUnknownBlock2, "Unknown Block 2");

	        _tagNameMap.put(TagSensorCalibration, "Sensor Calibration");

	        _tagNameMap.put(TagNoiseReduction2, "Noise Reduction 2");
	        _tagNameMap.put(TagDistortionCorrection2, "Distortion Correction 2");
	        _tagNameMap.put(TagShadingCompensation2, "Shading Compensation 2");
	        _tagNameMap.put(TagMultipleExposureMode, "Multiple Exposure Mode");
	        _tagNameMap.put(TagUnknownBlock3, "Unknown Block 3");
	        _tagNameMap.put(TagUnknownBlock4, "Unknown Block 4");
	        _tagNameMap.put(TagAspectRatio, "Aspect Ratio");
	        _tagNameMap.put(TagAspectFrame, "Aspect Frame");
	        _tagNameMap.put(TagFacesDetected, "Faces Detected");
	        _tagNameMap.put(TagFaceDetectArea, "Face Detect Area");
	        _tagNameMap.put(TagMaxFaces, "Max Faces");
	        _tagNameMap.put(TagFaceDetectFrameSize, "Face Detect Frame Size");
	        _tagNameMap.put(TagFaceDetectFrameCrop, "Face Detect Frame Crop");
	        _tagNameMap.put(TagCameraTemperature , "Camera Temperature");
	        _tagNameMap.put(TagKeystoneCompensation, "Keystone Compensation");
	        _tagNameMap.put(TagKeystoneDirection, "Keystone Direction");
	        _tagNameMap.put(TagKeystoneValue, "Keystone Value");
	    }

	    public OlympusImageProcessingMakernoteDirectory()
	    {
	        this.setDescriptor(new OlympusImageProcessingMakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Olympus Image Processing";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable String representations of tag values stored in a {@link OlympusFocusInfoMakernoteDirectory}.
	 * <p>
	 * Some Description functions converted from Exiftool version 10.10 created by Phil Harvey
	 * http://www.sno.phy.queensu.ca/~phil/exiftool/
	 * lib\Image\ExifTool\Olympus.pm
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusFocusInfoMakernoteDescriptor extends TagDescriptor<OlympusFocusInfoMakernoteDirectory>
	{
	    public OlympusFocusInfoMakernoteDescriptor(@NotNull OlympusFocusInfoMakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case OlympusFocusInfoMakernoteDirectory.TagFocusInfoVersion:
	                return getFocusInfoVersionDescription();
	            case OlympusFocusInfoMakernoteDirectory.TagAutoFocus:
	                return getAutoFocusDescription();
	            case OlympusFocusInfoMakernoteDirectory.TagFocusDistance:
	                return getFocusDistanceDescription();
	            case OlympusFocusInfoMakernoteDirectory.TagAfPoint:
	                return getAfPointDescription();
	            case OlympusFocusInfoMakernoteDirectory.TagExternalFlash:
	                return getExternalFlashDescription();
	            case OlympusFocusInfoMakernoteDirectory.TagExternalFlashBounce:
	                return getExternalFlashBounceDescription();
	            case OlympusFocusInfoMakernoteDirectory.TagExternalFlashZoom:
	                return getExternalFlashZoomDescription();
	            case OlympusFocusInfoMakernoteDirectory.TagManualFlash:
	                return getManualFlashDescription();
	            case OlympusFocusInfoMakernoteDirectory.TagMacroLed:
	                return getMacroLedDescription();
	            case OlympusFocusInfoMakernoteDirectory.TagSensorTemperature:
	                return getSensorTemperatureDescription();
	            case OlympusFocusInfoMakernoteDirectory.TagImageStabilization:
	                return getImageStabilizationDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getFocusInfoVersionDescription()
	    {
	        return getVersionBytesDescription(OlympusFocusInfoMakernoteDirectory.TagFocusInfoVersion, 4);
	    }

	    @Nullable
	    public String getAutoFocusDescription()
	    {
	        return getIndexedDescription(OlympusFocusInfoMakernoteDirectory.TagAutoFocus,
	            "Off", "On");
	    }

	    @Nullable
	    public String getFocusDistanceDescription()
	    {
	        Rational value = _directory.getRational(OlympusFocusInfoMakernoteDirectory.TagFocusDistance);
	        if (value == null)
	            return "inf";
	        if (value.getNumerator() == 0xFFFFFFFFL || value.getNumerator() == 0x00000000L)
	            return "inf";

	        return value.getNumerator() / 1000.0 + " m";
	    }

	    @Nullable
	    public String getAfPointDescription()
	    {
	        Integer value = _directory.getInteger(OlympusFocusInfoMakernoteDirectory.TagAfPoint);
	        if (value == null)
	            return null;

	        return value.toString();
	    }

	    @Nullable
	    public String getExternalFlashDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusFocusInfoMakernoteDirectory.TagExternalFlash);
	        if (values == null || values.length < 2)
	            return null;

	        String join = String.format("%d %d", (short)values[0], (short)values[1]);

	        if(join.equals("0 0"))
	            return "Off";
	        else if(join.equals("1 0"))
	            return "On";
	        else
	            return "Unknown (" + join + ")";
	    }

	    @Nullable
	    public String getExternalFlashBounceDescription()
	    {
	        return getIndexedDescription(OlympusFocusInfoMakernoteDirectory.TagExternalFlashBounce,
	                "Bounce or Off", "Direct");
	    }

	    @Nullable
	    public String getExternalFlashZoomDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusFocusInfoMakernoteDirectory.TagExternalFlashZoom);
	        if (values == null)
	        {
	            // check if it's only one value long also
	            Integer value = _directory.getInteger(OlympusFocusInfoMakernoteDirectory.TagExternalFlashZoom);
	            if(value == null)
	                return null;

	            values = new int[1];
	            values[0] = value;
	        }

	        if (values.length == 0)
	            return null;

	        String join = String.format("%d", (short)values[0]);
	        if(values.length > 1)
	            join += " " + String.format("%d", (short)values[1]);

	        if(join.equals("0"))
	            return "Off";
	        else if(join.equals("1"))
	            return "On";
	        else if(join.equals("0 0"))
	            return "Off";
	        else if(join.equals("1 0"))
	            return "On";
	        else
	            return "Unknown (" + join + ")";

	    }

	    @Nullable
	    public String getManualFlashDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusFocusInfoMakernoteDirectory.TagManualFlash);
	        if (values == null)
	            return null;

	        if ((short)values[0] == 0)
	            return "Off";

	        if ((short)values[1] == 1)
	            return "Full";
	        return "On (1/" + (short)values[1] + " strength)";
	    }

	    @Nullable
	    public String getMacroLedDescription()
	    {
	        return getIndexedDescription(OlympusFocusInfoMakernoteDirectory.TagMacroLed,
	                "Off", "On");
	    }

	    /// <remarks>
	    /// <para>TODO: Complete when Camera Model is available.</para>
	    /// <para>There are differences in how to interpret this tag that can only be reconciled by knowing the model.</para>
	    /// </remarks>
	    @Nullable
	    public String getSensorTemperatureDescription()
	    {
	        return _directory.getString(OlympusFocusInfoMakernoteDirectory.TagSensorTemperature);
	    }

	    @Nullable
	    public String getImageStabilizationDescription()
	    {
	        byte[] values = _directory.getByteArray(OlympusFocusInfoMakernoteDirectory.TagImageStabilization);
	        if (values == null)
	            return null;

	        if((values[0] | values[1] | values[2] | values[3]) == 0x0)
	            return "Off";
	        return "On, " + ((values[43] & 1) > 0 ? "Mode 1" : "Mode 2");
	    }
	}


	/**
	 * The Olympus focus info makernote is used by many manufacturers (Epson, Konica, Minolta and Agfa...), and as such contains some tags
	 * that appear specific to those manufacturers.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusFocusInfoMakernoteDirectory extends Directory
	{
	    public static final int TagFocusInfoVersion = 0x0000;
	    public static final int TagAutoFocus = 0x0209;
	    public static final int TagSceneDetect = 0x0210;
	    public static final int TagSceneArea = 0x0211;
	    public static final int TagSceneDetectData = 0x0212;

	    public static final int TagZoomStepCount = 0x0300;
	    public static final int TagFocusStepCount = 0x0301;
	    public static final int TagFocusStepInfinity = 0x0303;
	    public static final int TagFocusStepNear = 0x0304;
	    public static final int TagFocusDistance = 0x0305;
	    public static final int TagAfPoint = 0x0308;
	    // 0x031a Continuous AF parameters?
	    public static final int TagAfInfo = 0x0328;    // ifd

	    public static final int TagExternalFlash = 0x1201;
	    public static final int TagExternalFlashGuideNumber = 0x1203;
	    public static final int TagExternalFlashBounce = 0x1204;
	    public static final int TagExternalFlashZoom = 0x1205;
	    public static final int TagInternalFlash = 0x1208;
	    public static final int TagManualFlash = 0x1209;
	    public static final int TagMacroLed = 0x120A;

	    public static final int TagSensorTemperature = 0x1500;

	    public static final int TagImageStabilization = 0x1600;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TagFocusInfoVersion, "Focus Info Version");
	        _tagNameMap.put(TagAutoFocus, "Auto Focus");
	        _tagNameMap.put(TagSceneDetect, "Scene Detect");
	        _tagNameMap.put(TagSceneArea, "Scene Area");
	        _tagNameMap.put(TagSceneDetectData, "Scene Detect Data");
	        _tagNameMap.put(TagZoomStepCount, "Zoom Step Count");
	        _tagNameMap.put(TagFocusStepCount, "Focus Step Count");
	        _tagNameMap.put(TagFocusStepInfinity, "Focus Step Infinity");
	        _tagNameMap.put(TagFocusStepNear, "Focus Step Near");
	        _tagNameMap.put(TagFocusDistance, "Focus Distance");
	        _tagNameMap.put(TagAfPoint, "AF Point");
	        _tagNameMap.put(TagAfInfo, "AF Info");
	        _tagNameMap.put(TagExternalFlash, "External Flash");
	        _tagNameMap.put(TagExternalFlashGuideNumber, "External Flash Guide Number");
	        _tagNameMap.put(TagExternalFlashBounce, "External Flash Bounce");
	        _tagNameMap.put(TagExternalFlashZoom, "External Flash Zoom");
	        _tagNameMap.put(TagInternalFlash, "Internal Flash");
	        _tagNameMap.put(TagManualFlash, "Manual Flash");
	        _tagNameMap.put(TagMacroLed, "Macro LED");
	        _tagNameMap.put(TagSensorTemperature, "Sensor Temperature");
	        _tagNameMap.put(TagImageStabilization, "Image Stabilization");
	    }

	    public OlympusFocusInfoMakernoteDirectory()
	    {
	        this.setDescriptor(new OlympusFocusInfoMakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Olympus Focus Info";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable String representations of tag values stored in a {@link OlympusRawInfoMakernoteDirectory}.
	 * <p>
	 * Some Description functions converted from Exiftool version 10.33 created by Phil Harvey
	 * http://www.sno.phy.queensu.ca/~phil/exiftool/
	 * lib\Image\ExifTool\Olympus.pm
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusRawInfoMakernoteDescriptor extends TagDescriptor<OlympusRawInfoMakernoteDirectory>
	{
	    public OlympusRawInfoMakernoteDescriptor(@NotNull OlympusRawInfoMakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case OlympusRawInfoMakernoteDirectory.TagRawInfoVersion:
	                return getVersionBytesDescription(OlympusRawInfoMakernoteDirectory.TagRawInfoVersion, 4);
	            case OlympusRawInfoMakernoteDirectory.TagColorMatrix2:
	                return getColorMatrix2Description();
	            case OlympusRawInfoMakernoteDirectory.TagYCbCrCoefficients:
	                return getYCbCrCoefficientsDescription();
	            case OlympusRawInfoMakernoteDirectory.TagLightSource:
	                return getOlympusLightSourceDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getColorMatrix2Description()
	    {
	        int[] values = _directory.getIntArray(OlympusRawInfoMakernoteDirectory.TagColorMatrix2);
	        if (values == null)
	            return null;

	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < values.length; i++) {
	            sb.append((short)values[i]);
	            if (i < values.length - 1)
	                sb.append(" ");
	        }
	        return sb.length() == 0 ? null : sb.toString();
	    }

	    @Nullable
	    public String getYCbCrCoefficientsDescription()
	    {
	        int[] values = _directory.getIntArray(OlympusRawInfoMakernoteDirectory.TagYCbCrCoefficients);
	        if (values == null)
	            return null;

	        Rational[] ret = new Rational[values.length / 2];
	        for(int i = 0; i < values.length / 2; i++)
	        {
	            ret[i] = new Rational((short)values[2*i], (short)values[2*i + 1]);
	        }

	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < ret.length; i++) {
	            sb.append(ret[i].doubleValue());
	            if (i < ret.length - 1)
	                sb.append(" ");
	        }
	        return sb.length() == 0 ? null : sb.toString();
	    }
	    
	    @Nullable
	    public String getOlympusLightSourceDescription()
	    {
	        Integer value = _directory.getInteger(OlympusRawInfoMakernoteDirectory.TagLightSource);
	        if (value == null)
	            return null;

	        switch (value.shortValue())
	        {
	            case 0:
	                return "Unknown";
	            case 16:
	                return "Shade";
	            case 17:
	                return "Cloudy";
	            case 18:
	                return "Fine Weather";
	            case 20:
	                return "Tungsten (Incandescent)";
	            case 22:
	                return "Evening Sunlight";
	            case 33:
	                return "Daylight Fluorescent";
	            case 34:
	                return "Day White Fluorescent";
	            case 35:
	                return "Cool White Fluorescent";
	            case 36:
	                return "White Fluorescent";
	            case 256:
	                return "One Touch White Balance";
	            case 512:
	                return "Custom 1-4";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }
	}

	
	/**
	 * These tags are found only in ORF images of some models (eg. C8080WZ)
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class OlympusRawInfoMakernoteDirectory extends Directory
	{
	    public static final int TagRawInfoVersion = 0x0000;
	    public static final int TagWbRbLevelsUsed = 0x0100;
	    public static final int TagWbRbLevelsAuto = 0x0110;
	    public static final int TagWbRbLevelsShade = 0x0120;
	    public static final int TagWbRbLevelsCloudy = 0x0121;
	    public static final int TagWbRbLevelsFineWeather = 0x0122;
	    public static final int TagWbRbLevelsTungsten = 0x0123;
	    public static final int TagWbRbLevelsEveningSunlight = 0x0124;
	    public static final int TagWbRbLevelsDaylightFluor = 0x0130;
	    public static final int TagWbRbLevelsDayWhiteFluor = 0x0131;
	    public static final int TagWbRbLevelsCoolWhiteFluor = 0x0132;
	    public static final int TagWbRbLevelsWhiteFluorescent = 0x0133;

	    public static final int TagColorMatrix2 = 0x0200;
	    public static final int TagCoringFilter = 0x0310;
	    public static final int TagCoringValues = 0x0311;
	    public static final int TagBlackLevel2 = 0x0600;
	    public static final int TagYCbCrCoefficients = 0x0601;
	    public static final int TagValidPixelDepth = 0x0611;
	    public static final int TagCropLeft = 0x0612;
	    public static final int TagCropTop = 0x0613;
	    public static final int TagCropWidth = 0x0614;
	    public static final int TagCropHeight = 0x0615;

	    public static final int TagLightSource = 0x1000;

	    //the following 5 tags all have 3 values: val, min, max
	    public static final int TagWhiteBalanceComp = 0x1001;
	    public static final int TagSaturationSetting = 0x1010;
	    public static final int TagHueSetting = 0x1011;
	    public static final int TagContrastSetting = 0x1012;
	    public static final int TagSharpnessSetting = 0x1013;

	    // settings written by Camedia Master 4.x
	    public static final int TagCmExposureCompensation = 0x2000;
	    public static final int TagCmWhiteBalance = 0x2001;
	    public static final int TagCmWhiteBalanceComp = 0x2002;
	    public static final int TagCmWhiteBalanceGrayPoint = 0x2010;
	    public static final int TagCmSaturation = 0x2020;
	    public static final int TagCmHue = 0x2021;
	    public static final int TagCmContrast = 0x2022;
	    public static final int TagCmSharpness = 0x2023;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	     {
	        _tagNameMap.put(TagRawInfoVersion, "Raw Info Version");
	        _tagNameMap.put(TagWbRbLevelsUsed, "WB RB Levels Used");
	        _tagNameMap.put(TagWbRbLevelsAuto, "WB RB Levels Auto");
	        _tagNameMap.put(TagWbRbLevelsShade, "WB RB Levels Shade");
	        _tagNameMap.put(TagWbRbLevelsCloudy, "WB RB Levels Cloudy");
	        _tagNameMap.put(TagWbRbLevelsFineWeather, "WB RB Levels Fine Weather");
	        _tagNameMap.put(TagWbRbLevelsTungsten, "WB RB Levels Tungsten");
	        _tagNameMap.put(TagWbRbLevelsEveningSunlight, "WB RB Levels Evening Sunlight");
	        _tagNameMap.put(TagWbRbLevelsDaylightFluor, "WB RB Levels Daylight Fluor");
	        _tagNameMap.put(TagWbRbLevelsDayWhiteFluor, "WB RB Levels Day White Fluor");
	        _tagNameMap.put(TagWbRbLevelsCoolWhiteFluor, "WB RB Levels Cool White Fluor");
	        _tagNameMap.put(TagWbRbLevelsWhiteFluorescent, "WB RB Levels White Fluorescent");
	        _tagNameMap.put(TagColorMatrix2, "Color Matrix 2");
	        _tagNameMap.put(TagCoringFilter, "Coring Filter");
	        _tagNameMap.put(TagCoringValues, "Coring Values");
	        _tagNameMap.put(TagBlackLevel2, "Black Level 2");
	        _tagNameMap.put(TagYCbCrCoefficients, "YCbCrCoefficients");
	        _tagNameMap.put(TagValidPixelDepth, "Valid Pixel Depth");
	        _tagNameMap.put(TagCropLeft, "Crop Left");
	        _tagNameMap.put(TagCropTop, "Crop Top");
	        _tagNameMap.put(TagCropWidth, "Crop Width");
	        _tagNameMap.put(TagCropHeight, "Crop Height");
	        _tagNameMap.put(TagLightSource, "Light Source");

	        _tagNameMap.put(TagWhiteBalanceComp, "White Balance Comp");
	        _tagNameMap.put(TagSaturationSetting, "Saturation Setting");
	        _tagNameMap.put(TagHueSetting, "Hue Setting");
	        _tagNameMap.put(TagContrastSetting, "Contrast Setting");
	        _tagNameMap.put(TagSharpnessSetting, "Sharpness Setting");

	        _tagNameMap.put(TagCmExposureCompensation, "CM Exposure Compensation");
	        _tagNameMap.put(TagCmWhiteBalance, "CM White Balance");
	        _tagNameMap.put(TagCmWhiteBalanceComp, "CM White Balance Comp");
	        _tagNameMap.put(TagCmWhiteBalanceGrayPoint, "CM White Balance Gray Point");
	        _tagNameMap.put(TagCmSaturation, "CM Saturation");
	        _tagNameMap.put(TagCmHue, "CM Hue");
	        _tagNameMap.put(TagCmContrast, "CM Contrast");
	        _tagNameMap.put(TagCmSharpness, "CM Sharpness");
	    }

	    public OlympusRawInfoMakernoteDirectory()
	    {
	        this.setDescriptor(new OlympusRawInfoMakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Olympus Raw Info";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link PanasonicRawWbInfoDirectory}.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class PanasonicRawWbInfoDescriptor extends TagDescriptor<PanasonicRawWbInfoDirectory>
	{
	    public PanasonicRawWbInfoDescriptor(@NotNull PanasonicRawWbInfoDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case PanasonicRawWbInfoDirectory.TagWbType1:
	            case PanasonicRawWbInfoDirectory.TagWbType2:
	            case PanasonicRawWbInfoDirectory.TagWbType3:
	            case PanasonicRawWbInfoDirectory.TagWbType4:
	            case PanasonicRawWbInfoDirectory.TagWbType5:
	            case PanasonicRawWbInfoDirectory.TagWbType6:
	            case PanasonicRawWbInfoDirectory.TagWbType7:
	                return getWbTypeDescription(tagType);
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getWbTypeDescription(int tagType)
	    {
	        Integer value = _directory.getInteger(tagType);
	        if (value == null)
	            return null;
	        ExifDescriptorBase<PanasonicRawWbInfoDirectory> eb = new ExifDescriptorBase<PanasonicRawWbInfoDirectory>(_directory);
	        return eb.getWhiteBalanceDescription(value);
	    }
	}


	/**
	 * These tags can be found in Panasonic/Leica RAW, RW2 and RWL images. The index values are 'fake' but
	 * chosen specifically to make processing easier
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class PanasonicRawWbInfoDirectory extends Directory
	{
	    public static final int TagNumWbEntries = 0;

	    public static final int TagWbType1 = 1;
	    public static final int TagWbRbLevels1 = 2;

	    public static final int TagWbType2 = 4;
	    public static final int TagWbRbLevels2 = 5;

	    public static final int TagWbType3 = 7;
	    public static final int TagWbRbLevels3 = 8;

	    public static final int TagWbType4 = 10;
	    public static final int TagWbRbLevels4 = 11;

	    public static final int TagWbType5 = 13;
	    public static final int TagWbRbLevels5 = 14;

	    public static final int TagWbType6 = 16;
	    public static final int TagWbRbLevels6 = 17;

	    public static final int TagWbType7 = 19;
	    public static final int TagWbRbLevels7 = 20;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    {
	        _tagNameMap.put(TagNumWbEntries, "Num WB Entries");
	        _tagNameMap.put(TagWbType1, "WB Type 1");
	        _tagNameMap.put(TagWbRbLevels1, "WB RGB Levels 1");
	        _tagNameMap.put(TagWbType2, "WB Type 2");
	        _tagNameMap.put(TagWbRbLevels2, "WB RGB Levels 2");
	        _tagNameMap.put(TagWbType3, "WB Type 3");
	        _tagNameMap.put(TagWbRbLevels3, "WB RGB Levels 3");
	        _tagNameMap.put(TagWbType4, "WB Type 4");
	        _tagNameMap.put(TagWbRbLevels4, "WB RGB Levels 4");
	        _tagNameMap.put(TagWbType5, "WB Type 5");
	        _tagNameMap.put(TagWbRbLevels5, "WB RGB Levels 5");
	        _tagNameMap.put(TagWbType6, "WB Type 6");
	        _tagNameMap.put(TagWbRbLevels6, "WB RGB Levels 6");
	        _tagNameMap.put(TagWbType7, "WB Type 7");
	        _tagNameMap.put(TagWbRbLevels7, "WB RGB Levels 7");
	    }

	    public PanasonicRawWbInfoDirectory()
	    {
	        this.setDescriptor(new PanasonicRawWbInfoDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "PanasonicRaw WbInfo";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link PanasonicRawWbInfo2Directory}.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class PanasonicRawWbInfo2Descriptor extends TagDescriptor<PanasonicRawWbInfo2Directory>
	{
	    public PanasonicRawWbInfo2Descriptor(@NotNull PanasonicRawWbInfo2Directory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case PanasonicRawWbInfo2Directory.TagWbType1:
	            case PanasonicRawWbInfo2Directory.TagWbType2:
	            case PanasonicRawWbInfo2Directory.TagWbType3:
	            case PanasonicRawWbInfo2Directory.TagWbType4:
	            case PanasonicRawWbInfo2Directory.TagWbType5:
	            case PanasonicRawWbInfo2Directory.TagWbType6:
	            case PanasonicRawWbInfo2Directory.TagWbType7:
	                return getWbTypeDescription(tagType);
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getWbTypeDescription(int tagType)
	    {
	        Integer value = _directory.getInteger(tagType);
	        if (value == null)
	            return null;
	        ExifDescriptorBase<PanasonicRawWbInfo2Directory> eb = new ExifDescriptorBase<PanasonicRawWbInfo2Directory>(_directory);
	        return eb.getWhiteBalanceDescription(value);
	    }
	}

	
	/**
	 * These tags can be found in Panasonic/Leica RAW, RW2 and RWL images. The index values are 'fake' but
	 * chosen specifically to make processing easier
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class PanasonicRawWbInfo2Directory extends Directory
	{
	    public static final int TagNumWbEntries = 0;

	    public static final int TagWbType1 = 1;
	    public static final int TagWbRgbLevels1 = 2;

	    public static final int TagWbType2 = 5;
	    public static final int TagWbRgbLevels2 = 6;

	    public static final int TagWbType3 = 9;
	    public static final int TagWbRgbLevels3 = 10;

	    public static final int TagWbType4 = 13;
	    public static final int TagWbRgbLevels4 = 14;

	    public static final int TagWbType5 = 17;
	    public static final int TagWbRgbLevels5 = 18;

	    public static final int TagWbType6 = 21;
	    public static final int TagWbRgbLevels6 = 22;

	    public static final int TagWbType7 = 25;
	    public static final int TagWbRgbLevels7 = 26;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    {
	        _tagNameMap.put(TagNumWbEntries, "Num WB Entries");
	        _tagNameMap.put(TagNumWbEntries, "Num WB Entries");
	        _tagNameMap.put(TagWbType1, "WB Type 1");
	        _tagNameMap.put(TagWbRgbLevels1, "WB RGB Levels 1");
	        _tagNameMap.put(TagWbType2, "WB Type 2");
	        _tagNameMap.put(TagWbRgbLevels2, "WB RGB Levels 2");
	        _tagNameMap.put(TagWbType3, "WB Type 3");
	        _tagNameMap.put(TagWbRgbLevels3, "WB RGB Levels 3");
	        _tagNameMap.put(TagWbType4, "WB Type 4");
	        _tagNameMap.put(TagWbRgbLevels4, "WB RGB Levels 4");
	        _tagNameMap.put(TagWbType5, "WB Type 5");
	        _tagNameMap.put(TagWbRgbLevels5, "WB RGB Levels 5");
	        _tagNameMap.put(TagWbType6, "WB Type 6");
	        _tagNameMap.put(TagWbRgbLevels6, "WB RGB Levels 6");
	        _tagNameMap.put(TagWbType7, "WB Type 7");
	        _tagNameMap.put(TagWbRgbLevels7, "WB RGB Levels 7");
	    }

	    public PanasonicRawWbInfo2Directory()
	    {
	        this.setDescriptor(new PanasonicRawWbInfo2Descriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "PanasonicRaw WbInfo2";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link PanasonicRawDistortionDirectory}.
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class PanasonicRawDistortionDescriptor extends TagDescriptor<PanasonicRawDistortionDirectory>
	{
	    public PanasonicRawDistortionDescriptor(@NotNull PanasonicRawDistortionDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case PanasonicRawDistortionDirectory.TagDistortionParam02:
	                return getDistortionParam02Description();
	            case PanasonicRawDistortionDirectory.TagDistortionParam04:
	                return getDistortionParam04Description();
	            case PanasonicRawDistortionDirectory.TagDistortionScale:
	                return getDistortionScaleDescription();
	            case PanasonicRawDistortionDirectory.TagDistortionCorrection:
	                return getDistortionCorrectionDescription();
	            case PanasonicRawDistortionDirectory.TagDistortionParam08:
	                return getDistortionParam08Description();
	            case PanasonicRawDistortionDirectory.TagDistortionParam09:
	                return getDistortionParam09Description();
	            case PanasonicRawDistortionDirectory.TagDistortionParam11:
	                return getDistortionParam11Description();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getDistortionParam02Description()
	    {
	        Integer value = _directory.getInteger(PanasonicRawDistortionDirectory.TagDistortionParam02);
	        if (value == null)
	            return null;

	        return new Rational(value, 32678).toString();
	    }

	    @Nullable
	    public String getDistortionParam04Description()
	    {
	        Integer value = _directory.getInteger(PanasonicRawDistortionDirectory.TagDistortionParam04);
	        if (value == null)
	            return null;

	        return new Rational(value, 32678).toString();
	    }

	    @Nullable
	    public String getDistortionScaleDescription()
	    {
	        Integer value = _directory.getInteger(PanasonicRawDistortionDirectory.TagDistortionScale);
	        if (value == null)
	            return null;

	        //return (1 / (1 + value / 32768)).toString();
	        return Integer.toString(1 / (1 + value / 32768));
	    }

	    @Nullable
	    public String getDistortionCorrectionDescription()
	    {
	        Integer value = _directory.getInteger(PanasonicRawDistortionDirectory.TagDistortionCorrection);
	        if (value == null)
	            return null;

	        // (have seen the upper 4 bits set for GF5 and GX1, giving a value of -4095 - PH)
	        int mask = 0x000f;
	        switch (value & mask)
	        {
	            case 0:
	                return "Off";
	            case 1:
	                return "On";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getDistortionParam08Description()
	    {
	        Integer value = _directory.getInteger(PanasonicRawDistortionDirectory.TagDistortionParam08);
	        if (value == null)
	            return null;

	        return new Rational(value, 32678).toString();
	    }

	    @Nullable
	    public String getDistortionParam09Description()
	    {
	        Integer value = _directory.getInteger(PanasonicRawDistortionDirectory.TagDistortionParam09);
	        if (value == null)
	            return null;

	        return new Rational(value, 32678).toString();
	    }

	    @Nullable
	    public String getDistortionParam11Description()
	    {
	        Integer value = _directory.getInteger(PanasonicRawDistortionDirectory.TagDistortionParam11);
	        if (value == null)
	            return null;

	        return new Rational(value, 32678).toString();
	    }
	}


	/**
	 * These tags can be found in Panasonic/Leica RAW, RW2 and RWL images. The index values are 'fake' but
	 * chosen specifically to make processing easier
	 *
	 * @author Kevin Mott https://github.com/kwhopper
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class PanasonicRawDistortionDirectory extends Directory
	{
	    // 0 and 1 are checksums

	    public static final int TagDistortionParam02 = 2;

	    public static final int TagDistortionParam04 = 4;
	    public static final int TagDistortionScale = 5;

	    public static final int TagDistortionCorrection = 7;
	    public static final int TagDistortionParam08 = 8;
	    public static final int TagDistortionParam09 = 9;

	    public static final int TagDistortionParam11 = 11;
	    public static final int TagDistortionN = 12;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    {
	        _tagNameMap.put(TagDistortionParam02, "Distortion Param 2");
	        _tagNameMap.put(TagDistortionParam04, "Distortion Param 4");
	        _tagNameMap.put(TagDistortionScale, "Distortion Scale");
	        _tagNameMap.put(TagDistortionCorrection, "Distortion Correction");
	        _tagNameMap.put(TagDistortionParam08, "Distortion Param 8");
	        _tagNameMap.put(TagDistortionParam09, "Distortion Param 9");
	        _tagNameMap.put(TagDistortionParam11, "Distortion Param 11");
	        _tagNameMap.put(TagDistortionN, "Distortion N");
	    }

	    public PanasonicRawDistortionDirectory()
	    {
	        this.setDescriptor(new PanasonicRawDistortionDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "PanasonicRaw DistortionInfo";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link SonyType1MakernoteDirectory}.
	 * Thanks to David Carson for the initial version of this class.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class SonyType1MakernoteDescriptor extends TagDescriptor<SonyType1MakernoteDirectory>
	{
	    public SonyType1MakernoteDescriptor(@NotNull SonyType1MakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case SonyType1MakernoteDirectory.TAG_IMAGE_QUALITY:
	                return getImageQualityDescription();
	            case SonyType1MakernoteDirectory.TAG_FLASH_EXPOSURE_COMP:
	                return getFlashExposureCompensationDescription();
	            case SonyType1MakernoteDirectory.TAG_TELECONVERTER:
	                return getTeleconverterDescription();
	            case SonyType1MakernoteDirectory.TAG_WHITE_BALANCE:
	                return getWhiteBalanceDescription();
	            case SonyType1MakernoteDirectory.TAG_COLOR_TEMPERATURE:
	                return getColorTemperatureDescription();
	            case SonyType1MakernoteDirectory.TAG_SCENE_MODE:
	                return getSceneModeDescription();
	            case SonyType1MakernoteDirectory.TAG_ZONE_MATCHING:
	                return getZoneMatchingDescription();
	            case SonyType1MakernoteDirectory.TAG_DYNAMIC_RANGE_OPTIMISER:
	                return getDynamicRangeOptimizerDescription();
	            case SonyType1MakernoteDirectory.TAG_IMAGE_STABILISATION:
	                return getImageStabilizationDescription();
	            // Unfortunately it seems that there is no definite mapping between a lens ID and a lens model
	            // http://gvsoft.homedns.org/exif/makernote-sony-type1.html#0xb027
//	            case TAG_LENS_ID:
//	                return getLensIDDescription();
	            case SonyType1MakernoteDirectory.TAG_COLOR_MODE:
	                return getColorModeDescription();
	            case SonyType1MakernoteDirectory.TAG_MACRO:
	                return getMacroDescription();
	            case SonyType1MakernoteDirectory.TAG_EXPOSURE_MODE:
	                return getExposureModeDescription();
	            case SonyType1MakernoteDirectory.TAG_JPEG_QUALITY:
	                return getJpegQualityDescription();
	            case SonyType1MakernoteDirectory.TAG_ANTI_BLUR:
	                return getAntiBlurDescription();
	            case SonyType1MakernoteDirectory.TAG_LONG_EXPOSURE_NOISE_REDUCTION_OR_FOCUS_MODE:
	                return getLongExposureNoiseReductionDescription();
	            case SonyType1MakernoteDirectory.TAG_HIGH_ISO_NOISE_REDUCTION:
	                return getHighIsoNoiseReductionDescription();
	            case SonyType1MakernoteDirectory.TAG_PICTURE_EFFECT:
	                return getPictureEffectDescription();
	            case SonyType1MakernoteDirectory.TAG_SOFT_SKIN_EFFECT:
	                return getSoftSkinEffectDescription();
	            case SonyType1MakernoteDirectory.TAG_VIGNETTING_CORRECTION:
	                return getVignettingCorrectionDescription();
	            case SonyType1MakernoteDirectory.TAG_LATERAL_CHROMATIC_ABERRATION:
	                return getLateralChromaticAberrationDescription();
	            case SonyType1MakernoteDirectory.TAG_DISTORTION_CORRECTION:
	                return getDistortionCorrectionDescription();
	            case SonyType1MakernoteDirectory.TAG_AUTO_PORTRAIT_FRAMED:
	                return getAutoPortraitFramedDescription();
	            case SonyType1MakernoteDirectory.TAG_FOCUS_MODE:
	                return getFocusModeDescription();
	            case SonyType1MakernoteDirectory.TAG_AF_POINT_SELECTED:
	                return getAFPointSelectedDescription();
	            case SonyType1MakernoteDirectory.TAG_SONY_MODEL_ID:
	                return getSonyModelIdDescription();
	            case SonyType1MakernoteDirectory.TAG_AF_MODE:
	                return getAFModeDescription();
	            case SonyType1MakernoteDirectory.TAG_AF_ILLUMINATOR:
	                return getAFIlluminatorDescription();
	            case SonyType1MakernoteDirectory.TAG_FLASH_LEVEL:
	                return getFlashLevelDescription();
	            case SonyType1MakernoteDirectory.TAG_RELEASE_MODE:
	                return getReleaseModeDescription();
	            case SonyType1MakernoteDirectory.TAG_SEQUENCE_NUMBER:
	                return getSequenceNumberDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getImageQualityDescription()
	    {
	        return getIndexedDescription(SonyType1MakernoteDirectory.TAG_IMAGE_QUALITY,
	            "RAW",
	            "Super Fine",
	            "Fine",
	            "Standard",
	            "Economy",
	            "Extra Fine",
	            "RAW + JPEG",
	            "Compressed RAW",
	            "Compressed RAW + JPEG");
	    }

	    @Nullable
	    public String getFlashExposureCompensationDescription()
	    {
	        return getFormattedInt(SonyType1MakernoteDirectory.TAG_FLASH_EXPOSURE_COMP, "%d EV");
	    }

	    @Nullable
	    public String getTeleconverterDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_TELECONVERTER);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0x00: return "None";
	            case 0x48: return "Minolta/Sony AF 2x APO (D)";
	            case 0x50: return "Minolta AF 2x APO II";
	            case 0x60: return "Minolta AF 2x APO";
	            case 0x88: return "Minolta/Sony AF 1.4x APO (D)";
	            case 0x90: return "Minolta AF 1.4x APO II";
	            case 0xa0: return "Minolta AF 1.4x APO";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getWhiteBalanceDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_WHITE_BALANCE);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0x00: return "Auto";
	            case 0x01: return "Color Temperature/Color Filter";
	            case 0x10: return "Daylight";
	            case 0x20: return "Cloudy";
	            case 0x30: return "Shade";
	            case 0x40: return "Tungsten";
	            case 0x50: return "Flash";
	            case 0x60: return "Fluorescent";
	            case 0x70: return "Custom";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getColorTemperatureDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_COLOR_TEMPERATURE);
	        if (value == null)
	            return null;
	        if (value == 0)
	            return "Auto";
	        int kelvin = ((value & 0x00FF0000) >> 8) | ((value & 0xFF000000) >> 24);
	        return String.format("%d K", kelvin);
	    }

	    @Nullable
	    public String getZoneMatchingDescription()
	    {
	        return getIndexedDescription(SonyType1MakernoteDirectory.TAG_ZONE_MATCHING,
	            "ISO Setting Used", "High Key", "Low Key");
	    }

	    @Nullable
	    public String getDynamicRangeOptimizerDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_DYNAMIC_RANGE_OPTIMISER);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 1: return "Standard";
	            case 2: return "Advanced Auto";
	            case 3: return "Auto";
	            case 8: return "Advanced LV1";
	            case 9: return "Advanced LV2";
	            case 10: return "Advanced LV3";
	            case 11: return "Advanced LV4";
	            case 12: return "Advanced LV5";
	            case 16: return "LV1";
	            case 17: return "LV2";
	            case 18: return "LV3";
	            case 19: return "LV4";
	            case 20: return "LV5";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getImageStabilizationDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_IMAGE_STABILISATION);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 1: return "On";
	            default: return "N/A";
	        }
	    }

	    @Nullable
	    public String getColorModeDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_COLOR_MODE);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Standard";
	            case 1: return "Vivid";
	            case 2: return "Portrait";
	            case 3: return "Landscape";
	            case 4: return "Sunset";
	            case 5: return "Night Portrait";
	            case 6: return "Black & White";
	            case 7: return "Adobe RGB";
	            case 12: case 100: return "Neutral";
	            case 13: case 101: return "Clear";
	            case 14: case 102: return "Deep";
	            case 15: case 103: return "Light";
	            case 16: return "Autumn";
	            case 17: return "Sepia";
	            case 104: return "Night View";
	            case 105: return "Autumn Leaves";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getMacroDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_MACRO);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 1: return "On";
	            case 2: return "Magnifying Glass/Super Macro";
	            case 0xFFFF: return "N/A";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getExposureModeDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_EXPOSURE_MODE);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Program";
	            case 1: return "Portrait";
	            case 2: return "Beach";
	            case 3: return "Sports";
	            case 4: return "Snow";
	            case 5: return "Landscape";
	            case 6: return "Auto";
	            case 7: return "Aperture Priority";
	            case 8: return "Shutter Priority";
	            case 9: return "Night Scene / Twilight";
	            case 10: return "Hi-Speed Shutter";
	            case 11: return "Twilight Portrait";
	            case 12: return "Soft Snap/Portrait";
	            case 13: return "Fireworks";
	            case 14: return "Smile Shutter";
	            case 15: return "Manual";
	            case 18: return "High Sensitivity";
	            case 19: return "Macro";
	            case 20: return "Advanced Sports Shooting";
	            case 29: return "Underwater";
	            case 33: return "Food";
	            case 34: return "Panorama";
	            case 35: return "Handheld Night Shot";
	            case 36: return "Anti Motion Blur";
	            case 37: return "Pet";
	            case 38: return "Backlight Correction HDR";
	            case 39: return "Superior Auto";
	            case 40: return "Background Defocus";
	            case 41: return "Soft Skin";
	            case 42: return "3D Image";
	            case 0xFFFF: return "N/A";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getJpegQualityDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_JPEG_QUALITY);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Normal";
	            case 1: return "Fine";
	            case 2: return "Extra Fine";
	            case 0xFFFF: return "N/A";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getAntiBlurDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_ANTI_BLUR);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 1: return "On (Continuous)";
	            case 2: return "On (Shooting)";
	            case 0xFFFF: return "N/A";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getLongExposureNoiseReductionDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_LONG_EXPOSURE_NOISE_REDUCTION_OR_FOCUS_MODE);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 1: return "On";
	            case 0xFFFF: return "N/A";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getHighIsoNoiseReductionDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_HIGH_ISO_NOISE_REDUCTION);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 1: return "On";
	            case 2: return "Normal";
	            case 3: return "High";
	            case 0x100: return "Auto";
	            case 0xffff: return "N/A";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getPictureEffectDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_PICTURE_EFFECT);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 1: return "Toy Camera";
	            case 2: return "Pop Color";
	            case 3: return "Posterization";
	            case 4: return "Posterization B/W";
	            case 5: return "Retro Photo";
	            case 6: return "Soft High Key";
	            case 7: return "Partial Color (red)";
	            case 8: return "Partial Color (green)";
	            case 9: return "Partial Color (blue)";
	            case 10: return "Partial Color (yellow)";
	            case 13: return "High Contrast Monochrome";
	            case 16: return "Toy Camera (normal)";
	            case 17: return "Toy Camera (cool)";
	            case 18: return "Toy Camera (warm)";
	            case 19: return "Toy Camera (green)";
	            case 20: return "Toy Camera (magenta)";
	            case 32: return "Soft Focus (low)";
	            case 33: return "Soft Focus";
	            case 34: return "Soft Focus (high)";
	            case 48: return "Miniature (auto)";
	            case 49: return "Miniature (top)";
	            case 50: return "Miniature (middle horizontal)";
	            case 51: return "Miniature (bottom)";
	            case 52: return "Miniature (left)";
	            case 53: return "Miniature (middle vertical)";
	            case 54: return "Miniature (right)";
	            case 64: return "HDR Painting (low)";
	            case 65: return "HDR Painting";
	            case 66: return "HDR Painting (high)";
	            case 80: return "Rich-tone Monochrome";
	            case 97: return "Water Color";
	            case 98: return "Water Color 2";
	            case 112: return "Illustration (low)";
	            case 113: return "Illustration";
	            case 114: return "Illustration (high)";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getSoftSkinEffectDescription()
	    {
	        return getIndexedDescription(SonyType1MakernoteDirectory.TAG_SOFT_SKIN_EFFECT, "Off", "Low", "Mid", "High");
	    }

	    @Nullable
	    public String getVignettingCorrectionDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_VIGNETTING_CORRECTION);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 2: return "Auto";
	            case 0xffffffff: return "N/A";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getLateralChromaticAberrationDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_LATERAL_CHROMATIC_ABERRATION);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 2: return "Auto";
	            case 0xffffffff: return "N/A";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getDistortionCorrectionDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_DISTORTION_CORRECTION);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 2: return "Auto";
	            case 0xffffffff: return "N/A";
	            default: return String.format("Unknown (%d)", value);
	        }
	    }

	    @Nullable
	    public String getAutoPortraitFramedDescription()
	    {
	        return getIndexedDescription(SonyType1MakernoteDirectory.TAG_AUTO_PORTRAIT_FRAMED, "No", "Yes");
	    }

	    @Nullable
	    public String getFocusModeDescription()
	    {
	        return getIndexedDescription(SonyType1MakernoteDirectory.TAG_FOCUS_MODE,
	            "Manual", null, "AF-A", "AF-C", "AF-S", null, "DMF", "AF-D");
	    }

	    @Nullable
	    public String getAFPointSelectedDescription()
	    {
	        return getIndexedDescription(SonyType1MakernoteDirectory.TAG_AF_POINT_SELECTED,
	            "Auto", // 0
	            "Center", // 1
	            "Top", // 2
	            "Upper-right", // 3
	            "Right", // 4
	            "Lower-right", // 5
	            "Bottom", // 6
	            "Lower-left", // 7
	            "Left", // 8
	            "Upper-left	  	", // 9
	            "Far Right", // 10
	            "Far Left", // 11
	            "Upper-middle", // 12
	            "Near Right", // 13
	            "Lower-middle", // 14
	            "Near Left", // 15
	            "Upper Far Right", // 16
	            "Lower Far Right", // 17
	            "Lower Far Left", // 18
	            "Upper Far Left" // 19
	        );
	    }

	    @Nullable
	    public String getSonyModelIdDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_SONY_MODEL_ID);

	        if (value == null)
	            return null;

	        switch (value) {
	            case 2: return "DSC-R1";
	            case 256: return "DSLR-A100";
	            case 257: return "DSLR-A900";
	            case 258: return "DSLR-A700";
	            case 259: return "DSLR-A200";
	            case 260: return "DSLR-A350";
	            case 261: return "DSLR-A300";
	            case 262: return "DSLR-A900 (APS-C mode)";
	            case 263: return "DSLR-A380/A390";
	            case 264: return "DSLR-A330";
	            case 265: return "DSLR-A230";
	            case 266: return "DSLR-A290";
	            case 269: return "DSLR-A850";
	            case 270: return "DSLR-A850 (APS-C mode)";
	            case 273: return "DSLR-A550";
	            case 274: return "DSLR-A500";
	            case 275: return "DSLR-A450";
	            case 278: return "NEX-5";
	            case 279: return "NEX-3";
	            case 280: return "SLT-A33";
	            case 281: return "SLT-A55V";
	            case 282: return "DSLR-A560";
	            case 283: return "DSLR-A580";
	            case 284: return "NEX-C3";
	            case 285: return "SLT-A35";
	            case 286: return "SLT-A65V";
	            case 287: return "SLT-A77V";
	            case 288: return "NEX-5N";
	            case 289: return "NEX-7";
	            case 290: return "NEX-VG20E";
	            case 291: return "SLT-A37";
	            case 292: return "SLT-A57";
	            case 293: return "NEX-F3";
	            case 294: return "SLT-A99V";
	            case 295: return "NEX-6";
	            case 296: return "NEX-5R";
	            case 297: return "DSC-RX100";
	            case 298: return "DSC-RX1";
	            case 299: return "NEX - VG900";
	            case 300: return "NEX - VG30E";
	            case 302: return "ILCE - 3000 / ILCE - 3500";
	            case 303: return "SLT - A58";
	            case 305: return "NEX - 3N";
	            case 306: return "ILCE-7";
	            case 307: return "NEX-5T";
	            case 308: return "DSC-RX100M2";
	            case 309: return "DSC-RX10";
	            case 310: return "DSC-RX1R";
	            case 311: return "ILCE-7R";
	            case 312: return "ILCE-6000";
	            case 313: return "ILCE-5000";
	            case 317: return "DSC-RX100M3";
	            case 318: return "ILCE-7S";
	            case 319: return "ILCA-77M2";
	            case 339: return "ILCE-5100";
	            case 340: return "ILCE-7M2";
	            case 341: return "DSC-RX100M4";
	            case 342: return "DSC-RX10M2";
	            case 344: return "DSC-RX1RM2";
	            case 346: return "ILCE-QX1";
	            case 347: return "ILCE-7RM2";
	            case 350: return "ILCE-7SM2";
	            case 353: return "ILCA-68";
	            case 354: return "ILCA-99M2";
	            case 355: return "DSC-RX10M3";
	            case 356: return "DSC-RX100M5";
	            case 357: return "ILCE-6300";
	            case 358: return "ILCE-9";
	            case 360: return "ILCE-6500";
	            case 362: return "ILCE-7RM3";
	            case 363: return "ILCE-7M3";
	            case 364: return "DSC-RX0";
	            case 365: return "DSC-RX10M4";
	            case 366: return "DSC-RX100M6";
	            case 367: return "DSC-HX99";
	            case 369: return "DSC-RX100M5A";
	            case 371: return "ILCE-6400";
	            case 372: return "DSC-RX0M2";
	            case 374: return "DSC-RX100M7";
	            case 375: return "ILCE-7RM4";
	            case 376: return "ILCE-9M2";
	            case 378: return "ILCE-6600";
	            case 379: return "ILCE-6100";
	            case 380: return "ZV-1";
	            case 381: return "ILCE-7C";
	            case 383: return "ILCE-7SM3";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getSceneModeDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_SCENE_MODE);

	        if (value == null)
	            return null;

	        switch (value) {
	            case 0: return "Standard";
	            case 1: return "Portrait";
	            case 2: return "Text";
	            case 3: return "Night Scene";
	            case 4: return "Sunset";
	            case 5: return "Sports";
	            case 6: return "Landscape";
	            case 7: return "Night Portrait";
	            case 8: return "Macro";
	            case 9: return "Super Macro";
	            case 16: return "Auto";
	            case 17: return "Night View/Portrait";
	            case 18: return "Sweep Panorama";
	            case 19: return "Handheld Night Shot";
	            case 20: return "Anti Motion Blur";
	            case 21: return "Cont. Priority AE";
	            case 22: return "Auto+";
	            case 23: return "3D Sweep Panorama";
	            case 24: return "Superior Auto";
	            case 25: return "High Sensitivity";
	            case 26: return "Fireworks";
	            case 27: return "Food";
	            case 28: return "Pet";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getAFModeDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_AF_MODE);

	        if (value == null)
	            return null;

	        switch (value) {
	            case 0: return "Default";
	            case 1: return "Multi";
	            case 2: return "Center";
	            case 3: return "Spot";
	            case 4: return "Flexible Spot";
	            case 6: return "Touch";
	            case 14: return "Manual Focus";
	            case 15: return "Face Detected";
	            case 0xffff: return "n/a";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getAFIlluminatorDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_AF_ILLUMINATOR);

	        if (value == null)
	            return null;

	        switch (value) {
	            case 0: return "Off";
	            case 1: return "Auto";
	            case 0xffff: return "n/a";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getFlashLevelDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_FLASH_LEVEL);

	        if (value == null)
	            return null;

	        switch (value) {
	            case -32768: return "Low";
	            case -3: return "-3/3";
	            case -2: return "-2/3";
	            case -1: return "-1/3";
	            case 0: return "Normal";
	            case 1: return "+1/3";
	            case 2: return "+2/3";
	            case 3: return "+3/3";
	            case 128: return "n/a";
	            case 32767: return "High";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getReleaseModeDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_RELEASE_MODE);

	        if (value == null)
	            return null;

	        switch (value) {
	            case 0: return "Normal";
	            case 2: return "Continuous";
	            case 5: return "Exposure Bracketing";
	            case 6: return "White Balance Bracketing";
	            case 65535: return "n/a";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getSequenceNumberDescription()
	    {
	        Integer value = _directory.getInteger(SonyType1MakernoteDirectory.TAG_RELEASE_MODE);

	        if (value == null)
	            return null;

	        switch (value) {
	            case 0: return "Single";
	            case 65535: return "n/a";
	            default:
	                return value.toString();
	        }
	    }
	}

	
	/**
	 * Describes tags specific to Sony cameras that use the Sony Type 1 makernote tags.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class SonyType1MakernoteDirectory extends Directory
	{
	    public static final int TAG_CAMERA_INFO = 0x0010;
	    public static final int TAG_FOCUS_INFO = 0x0020;

	    // https://exiftool.org/TagNames/Sony.html#Tag9050a
	    public static final int TAG_9050A = 0x0030;
	    // https://exiftool.org/TagNames/Sony.html#Tag9050b
	    public static final int TAG_9050B = 0x9050;
	    // https://exiftool.org/TagNames/Sony.html#Tag9050c
	    public static final int TAG_9050C = 0x0040;

	    public static final int TAG_IMAGE_QUALITY = 0x0102;
	    public static final int TAG_FLASH_EXPOSURE_COMP = 0x0104;
	    public static final int TAG_TELECONVERTER = 0x0105;

	    public static final int TAG_WHITE_BALANCE_FINE_TUNE = 0x0112;
	    public static final int TAG_CAMERA_SETTINGS = 0x0114;
	    public static final int TAG_WHITE_BALANCE = 0x0115;
	    public static final int TAG_EXTRA_INFO = 0x0116;

	    public static final int TAG_PRINT_IMAGE_MATCHING_INFO = 0x0E00;

	    public static final int TAG_MULTI_BURST_MODE = 0x1000;
	    public static final int TAG_MULTI_BURST_IMAGE_WIDTH = 0x1001;
	    public static final int TAG_MULTI_BURST_IMAGE_HEIGHT = 0x1002;
	    public static final int TAG_PANORAMA = 0x1003;

	    public static final int TAG_PREVIEW_IMAGE = 0x2001;
	    public static final int TAG_RATING = 0x2002;
	    public static final int TAG_CONTRAST = 0x2004;
	    public static final int TAG_SATURATION = 0x2005;
	    public static final int TAG_SHARPNESS = 0x2006;
	    public static final int TAG_BRIGHTNESS = 0x2007;
	    public static final int TAG_LONG_EXPOSURE_NOISE_REDUCTION = 0x2008;
	    public static final int TAG_HIGH_ISO_NOISE_REDUCTION = 0x2009;
	    public static final int TAG_HDR = 0x200a;
	    public static final int TAG_MULTI_FRAME_NOISE_REDUCTION = 0x200b;
	    public static final int TAG_PICTURE_EFFECT = 0x200e;
	    public static final int TAG_SOFT_SKIN_EFFECT = 0x200f;

	    public static final int TAG_VIGNETTING_CORRECTION = 0x2011;
	    public static final int TAG_LATERAL_CHROMATIC_ABERRATION = 0x2012;
	    public static final int TAG_DISTORTION_CORRECTION = 0x2013;
	    public static final int TAG_WB_SHIFT_AMBER_MAGENTA = 0x2014;
	    public static final int TAG_AUTO_PORTRAIT_FRAMED = 0x2016;
	    public static final int TAG_FOCUS_MODE = 0x201b;
	    public static final int TAG_AF_POINT_SELECTED = 0x201e;

	    public static final int TAG_SHOT_INFO = 0x3000;

	    public static final int TAG_FILE_FORMAT = 0xb000;
	    public static final int TAG_SONY_MODEL_ID = 0xb001;

	    public static final int TAG_COLOR_MODE_SETTING = 0xb020;
	    public static final int TAG_COLOR_TEMPERATURE = 0xb021;
	    public static final int TAG_COLOR_COMPENSATION_FILTER = 0xb022;
	    public static final int TAG_SCENE_MODE = 0xb023;
	    public static final int TAG_ZONE_MATCHING = 0xb024;
	    public static final int TAG_DYNAMIC_RANGE_OPTIMISER = 0xb025;
	    public static final int TAG_IMAGE_STABILISATION = 0xb026;
	    public static final int TAG_LENS_ID = 0xb027;
	    public static final int TAG_MINOLTA_MAKERNOTE = 0xb028;
	    public static final int TAG_COLOR_MODE = 0xb029;
	    public static final int TAG_LENS_SPEC = 0xb02a;
	    public static final int TAG_FULL_IMAGE_SIZE = 0xb02b;
	    public static final int TAG_PREVIEW_IMAGE_SIZE = 0xb02c;

	    public static final int TAG_MACRO = 0xb040;
	    public static final int TAG_EXPOSURE_MODE = 0xb041;
	    public static final int TAG_FOCUS_MODE_2 = 0xb042;
	    public static final int TAG_AF_MODE = 0xb043;
	    public static final int TAG_AF_ILLUMINATOR = 0xb044;
	    public static final int TAG_JPEG_QUALITY = 0xb047;
	    public static final int TAG_FLASH_LEVEL = 0xb048;
	    public static final int TAG_RELEASE_MODE = 0xb049;
	    public static final int TAG_SEQUENCE_NUMBER = 0xb04a;
	    public static final int TAG_ANTI_BLUR = 0xb04b;
	    /**
	     * (FocusMode for RX100)
	     * 0 = Manual
	     * 2 = AF-S
	     * 3 = AF-C
	     * 5 = Semi-manual
	     * 6 = Direct Manual Focus
	     * (LongExposureNoiseReduction for other models)
	     * 0 = Off
	     * 1 = On
	     * 2 = On 2
	     * 65535 = n/a
	     */
	    public static final int TAG_LONG_EXPOSURE_NOISE_REDUCTION_OR_FOCUS_MODE = 0xb04e;
	    public static final int TAG_DYNAMIC_RANGE_OPTIMIZER = 0xb04f;

	    public static final int TAG_HIGH_ISO_NOISE_REDUCTION_2 = 0xb050;
	    public static final int TAG_INTELLIGENT_AUTO = 0xb052;
	    public static final int TAG_WHITE_BALANCE_2 = 0xb054;

	    public static final int TAG_NO_PRINT = 0xFFFF;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    {
	        _tagNameMap.put(TAG_CAMERA_INFO, "Camera Info");
	        _tagNameMap.put(TAG_FOCUS_INFO, "Focus Info");

	        _tagNameMap.put(TAG_IMAGE_QUALITY, "Image Quality");
	        _tagNameMap.put(TAG_FLASH_EXPOSURE_COMP, "Flash Exposure Compensation");
	        _tagNameMap.put(TAG_TELECONVERTER, "Teleconverter Model");

	        _tagNameMap.put(TAG_WHITE_BALANCE_FINE_TUNE, "White Balance Fine Tune Value");
	        _tagNameMap.put(TAG_CAMERA_SETTINGS, "Camera Settings");
	        _tagNameMap.put(TAG_WHITE_BALANCE, "White Balance");
	        _tagNameMap.put(TAG_EXTRA_INFO, "Extra Info");

	        _tagNameMap.put(TAG_PRINT_IMAGE_MATCHING_INFO, "Print Image Matching (PIM) Info");

	        _tagNameMap.put(TAG_MULTI_BURST_MODE, "Multi Burst Mode");
	        _tagNameMap.put(TAG_MULTI_BURST_IMAGE_WIDTH, "Multi Burst Image Width");
	        _tagNameMap.put(TAG_MULTI_BURST_IMAGE_HEIGHT, "Multi Burst Image Height");
	        _tagNameMap.put(TAG_PANORAMA, "Panorama");

	        _tagNameMap.put(TAG_PREVIEW_IMAGE, "Preview Image");
	        _tagNameMap.put(TAG_RATING, "Rating");
	        _tagNameMap.put(TAG_CONTRAST, "Contrast");
	        _tagNameMap.put(TAG_SATURATION, "Saturation");
	        _tagNameMap.put(TAG_SHARPNESS, "Sharpness");
	        _tagNameMap.put(TAG_BRIGHTNESS, "Brightness");
	        _tagNameMap.put(TAG_LONG_EXPOSURE_NOISE_REDUCTION, "Long Exposure Noise Reduction");
	        _tagNameMap.put(TAG_HIGH_ISO_NOISE_REDUCTION, "High ISO Noise Reduction");
	        _tagNameMap.put(TAG_HDR, "HDR");
	        _tagNameMap.put(TAG_MULTI_FRAME_NOISE_REDUCTION, "Multi Frame Noise Reduction");
	        _tagNameMap.put(TAG_PICTURE_EFFECT, "Picture Effect");
	        _tagNameMap.put(TAG_SOFT_SKIN_EFFECT, "Soft Skin Effect");

	        _tagNameMap.put(TAG_VIGNETTING_CORRECTION, "Vignetting Correction");
	        _tagNameMap.put(TAG_LATERAL_CHROMATIC_ABERRATION, "Lateral Chromatic Aberration");
	        _tagNameMap.put(TAG_DISTORTION_CORRECTION, "Distortion Correction");
	        _tagNameMap.put(TAG_WB_SHIFT_AMBER_MAGENTA, "WB Shift Amber/Magenta");
	        _tagNameMap.put(TAG_AUTO_PORTRAIT_FRAMED, "Auto Portrait Framing");
	        _tagNameMap.put(TAG_FOCUS_MODE, "Focus Mode");
	        _tagNameMap.put(TAG_AF_POINT_SELECTED, "AF Point Selected");

	        _tagNameMap.put(TAG_SHOT_INFO, "Shot Info");

	        _tagNameMap.put(TAG_FILE_FORMAT, "File Format");
	        _tagNameMap.put(TAG_SONY_MODEL_ID, "Sony Model ID");

	        _tagNameMap.put(TAG_COLOR_MODE_SETTING, "Color Mode Setting");
	        _tagNameMap.put(TAG_COLOR_TEMPERATURE, "Color Temperature");
	        _tagNameMap.put(TAG_COLOR_COMPENSATION_FILTER, "Color Compensation Filter");
	        _tagNameMap.put(TAG_SCENE_MODE, "Scene Mode");
	        _tagNameMap.put(TAG_ZONE_MATCHING, "Zone Matching");
	        _tagNameMap.put(TAG_DYNAMIC_RANGE_OPTIMISER, "Dynamic Range Optimizer");
	        _tagNameMap.put(TAG_IMAGE_STABILISATION, "Image Stabilisation");
	        _tagNameMap.put(TAG_LENS_ID, "Lens ID");
	        _tagNameMap.put(TAG_MINOLTA_MAKERNOTE, "Minolta Makernote");
	        _tagNameMap.put(TAG_COLOR_MODE, "Color Mode");
	        _tagNameMap.put(TAG_LENS_SPEC, "Lens Spec");
	        _tagNameMap.put(TAG_FULL_IMAGE_SIZE, "Full Image Size");
	        _tagNameMap.put(TAG_PREVIEW_IMAGE_SIZE, "Preview Image Size");

	        _tagNameMap.put(TAG_MACRO, "Macro");
	        _tagNameMap.put(TAG_EXPOSURE_MODE, "Exposure Mode");
	        _tagNameMap.put(TAG_FOCUS_MODE_2, "Focus Mode");
	        _tagNameMap.put(TAG_AF_MODE, "AF Mode");
	        _tagNameMap.put(TAG_AF_ILLUMINATOR, "AF Illuminator");
	        _tagNameMap.put(TAG_JPEG_QUALITY, "Quality");
	        _tagNameMap.put(TAG_FLASH_LEVEL, "Flash Level");
	        _tagNameMap.put(TAG_RELEASE_MODE, "Release Mode");
	        _tagNameMap.put(TAG_SEQUENCE_NUMBER, "Sequence Number");
	        _tagNameMap.put(TAG_ANTI_BLUR, "Anti Blur");
	        _tagNameMap.put(TAG_LONG_EXPOSURE_NOISE_REDUCTION_OR_FOCUS_MODE, "Long Exposure Noise Reduction");
	        _tagNameMap.put(TAG_DYNAMIC_RANGE_OPTIMIZER, "Dynamic Range Optimizer");

	        _tagNameMap.put(TAG_HIGH_ISO_NOISE_REDUCTION_2, "High ISO Noise Reduction");
	        _tagNameMap.put(TAG_INTELLIGENT_AUTO, "Intelligent Auto");
	        _tagNameMap.put(TAG_WHITE_BALANCE_2, "White Balance 2");

	        _tagNameMap.put(TAG_NO_PRINT, "No Print");
	    }

	    public SonyType1MakernoteDirectory()
	    {
	        this.setDescriptor(new SonyType1MakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Sony Makernote";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}

	public abstract class SonyEncodedDataDirectoryBase extends Directory
	{
	    private final byte[] _substitution = new byte[]
	        {
	            0, 1, 50, (byte)177, 10, 14, (byte)135, 40, 2, (byte)204, (byte)202, (byte)173, 27, (byte)220, 8, (byte)237, 100,
	            (byte)134, (byte)240, 79, (byte)140, 108, (byte)184, (byte)203, 105, (byte)196, 44, 3, (byte)151, (byte)182,
	            (byte)147, 124, 20, (byte)243, (byte)226, 62, 48, (byte)142, (byte)215, 96, 28, (byte)161, (byte)171, 55,
	            (byte)236, 117, (byte)190, 35, 21, 106, 89, 63, (byte)208, (byte)185, (byte)150, (byte)181, 80, 39, (byte)136,
	            (byte)227, (byte)129, (byte)148, (byte)224, (byte)192, 4, 92, (byte)198, (byte)232, 95, 75, 112, 56, (byte)159,
	            (byte)130, (byte)128, 81, 43, (byte)197, 69, 73, (byte)155, 33, 82, 83, 84, (byte)133, 11, 93, 97, (byte)218,
	            123, 85, 38, 36, 7, 110, 54, 91, 71, (byte)183, (byte)217, 74, (byte)162, (byte)223, (byte)191, 18, 37,
	            (byte)188, 30, 127, 86, (byte)234, 16, (byte)230, (byte)207, 103, 77, 60, (byte)145, (byte)131, (byte)225, 49,
	            (byte)179, 111, (byte)244, 5, (byte)138, 70, (byte)200, 24, 118, 104, (byte)189, (byte)172, (byte)146, 42, 19,
	            (byte)233, 15, (byte)163, 122, (byte)219, 61, (byte)212, (byte)231, 58, 26, 87, (byte)175, 32, 66, (byte)178,
	            (byte)158, (byte)195, (byte)139, (byte)242, (byte)213, (byte)211, (byte)164, 126, 31, (byte)152, (byte)156,
	            (byte)238, 116, (byte)165, (byte)166, (byte)167, (byte)216, 94, (byte)176, (byte)180, 52, (byte)206, (byte)168,
	            121, 119, 90, (byte)193, (byte)137, (byte)174, (byte)154, 17, 51, (byte)157, (byte)245, 57, 25, 101, 120, 22,
	            113, (byte)210, (byte)169, 68, 99, 64, 41, (byte)186, (byte)160, (byte)143, (byte)228, (byte)214, 59, (byte)132,
	            13, (byte)194, 78, 88, (byte)221, (byte)153, 34, 107, (byte)201, (byte)187, 23, 6, (byte)229, 125, 102, 67, 98,
	            (byte)246, (byte)205, 53, (byte)144, 46, 65, (byte)141, 109, (byte)170, 9, 115, (byte)149, 12, (byte)241, 29,
	            (byte)222, 76, 47, 45, (byte)247, (byte)209, 114, (byte)235, (byte)239, 72, (byte)199, (byte)248, (byte)249,
	            (byte)250, (byte)251, (byte)252, (byte)253, (byte)254, (byte)255
	        };

	    protected void decipherInPlace(byte[] bytes)
	    {
	        for (int i = 0; i < bytes.length; i++) {
	            bytes[i] = _substitution[bytes[i] & 0xFF];
	        }
	    }
	}
	
	public class SonyTag9050bDescriptor extends TagDescriptor<SonyTag9050bDirectory>
	{
	    public SonyTag9050bDescriptor(@NotNull SonyTag9050bDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case SonyTag9050bDirectory.TAG_FLASH_STATUS:
	                return getFlashStatusDescription();
	            case SonyTag9050bDirectory.TAG_SONY_EXPOSURE_TIME:
	                return getSonyExposureTimeDescription();
	            case SonyTag9050bDirectory.TAG_INTERNAL_SERIAL_NUMBER:
	                return getInternalSerialNumberDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getInternalSerialNumberDescription()
	    {
	        int[] values = _directory.getIntArray(SonyTag9050bDirectory.TAG_INTERNAL_SERIAL_NUMBER);
	        if (values == null)
	            return null;
	        StringBuilder sn = new StringBuilder();
	        for (int value : values) {
	            sn.append(String.format("%02x", value));
	        }
	        return sn.toString();
	    }

	    @Nullable
	    public String getSonyExposureTimeDescription()
	    {
	        Float value = _directory.getFloatObject(SonyTag9050bDirectory.TAG_SONY_EXPOSURE_TIME);
	        if (value == null)
	            return null;
	        if (value == 0)
	            return "0";
	        return String.format("1/%s", (int)(0.5 + (1 / value)));
	    }

	    @Nullable
	    public String getFlashStatusDescription()
	    {

	        Integer value = _directory.getInteger(SonyTag9050bDirectory.TAG_FLASH_STATUS);
	        if (value == null)
	            return null;
	        switch (value) {
	            case 0x00:
	                return "No flash present";
	            case 0x02:
	                return "Flash inhibited";
	            case 0x40:
	                return "Built-in flash present";
	            case 0x41:
	                return "Built-in flash fired";
	            case 0x42:
	                return "Built-in flash inhibited";
	            case 0x80:
	                return "External flash present";
	            case 0x81:
	                return "External flash fired";
	            default:
	                return "Unknown (" + value + ")";
	        }
	    }
	}

	
	public class SonyTag9050bDirectory extends SonyEncodedDataDirectoryBase
	{
	    public static final int TAG_SHUTTER = 0x0026;
	    public static final int TAG_FLASH_STATUS = 0x0039;
	    public static final int TAG_SHUTTER_COUNT = 0x003a;
	    public static final int TAG_SONY_EXPOSURE_TIME = 0x0046;
	    public static final int TAG_SONY_F_NUMBER = 0x0048;
	    public static final int TAG_RELEASE_MODE_2 = 0x006d;
	    public static final int TAG_INTERNAL_SERIAL_NUMBER = 0x0088;
	    public static final int TAG_LENS_MOUNT = 0x0105;
	    public static final int TAG_LENS_FORMAT = 0x0106;
	    public static final int TAG_LENS_TYPE_2 = 0x0107;
	    public static final int TAG_DISTORTION_CORR_PARAMS_PRESENT = 0x010b;
	    public static final int TAG_APS_C_SIZE_CAPTURE = 0x0114;
	    public static final int TAG_LENS_SPEC_FEATURES = 0x0116;
	    public static final int TAG_SHUTTER_COUNT_3 = 0x019f;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    {
	        _tagNameMap.put(TAG_SHUTTER, "Shutter");
	        _tagNameMap.put(TAG_FLASH_STATUS, "Flash Status");
	        _tagNameMap.put(TAG_SHUTTER_COUNT, "Shutter Count");
	        _tagNameMap.put(TAG_SONY_EXPOSURE_TIME, "Sony Exposure Time");
	        _tagNameMap.put(TAG_SONY_F_NUMBER, "Sony F Number");
	        _tagNameMap.put(TAG_RELEASE_MODE_2, "Release Mode 2");
	        _tagNameMap.put(TAG_INTERNAL_SERIAL_NUMBER, "Internal Serial Number");
	        _tagNameMap.put(TAG_LENS_MOUNT, "Lens Mount");
	        _tagNameMap.put(TAG_LENS_FORMAT, "Lens Format");
	        _tagNameMap.put(TAG_LENS_TYPE_2, "Lens Type 2");
	        _tagNameMap.put(TAG_DISTORTION_CORR_PARAMS_PRESENT, "Distortion Corr Params Present");
	        _tagNameMap.put(TAG_APS_C_SIZE_CAPTURE, "APS-C Size Capture");
	        _tagNameMap.put(TAG_LENS_SPEC_FEATURES, "Lens Spec Features");
	        _tagNameMap.put(TAG_SHUTTER_COUNT_3, "Shutter Count 3");
	    }

	    public SonyTag9050bDirectory()
	    {
	        this.setDescriptor(new SonyTag9050bDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Sony 9050B";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }

	    public void read(byte[] bytes)
	    {
	        //SonyTag9050bDirectory dir = new SonyTag9050bDirectory();

	        try {
	            // First, decipher the bytes
	            decipherInPlace(bytes);

	            ByteArrayReader reader = new ByteArrayReader(bytes);
	            reader.setMotorolaByteOrder(false);

	            // Shutter
	            int offset = TAG_SHUTTER;
	            int shutter0 = reader.getUInt16(offset);
	            int shutter1 = reader.getUInt16(offset + 2);
	            int shutter2 = reader.getUInt16(offset + 4);
	            setIntArray(TAG_SHUTTER, new int[]{shutter0, shutter1, shutter2});

	            // FlashStatus
	            offset = TAG_FLASH_STATUS;
	            int flashStatus = reader.getUInt8(offset);
	            setInt(TAG_FLASH_STATUS, flashStatus);

	            // ShutterCount
	            offset = TAG_SHUTTER_COUNT;
	            long shutterCount = reader.getUInt32(offset);
	            setLong(TAG_SHUTTER_COUNT, shutterCount);

	            // SonyExposureTime
	            offset = TAG_SONY_EXPOSURE_TIME;
	            int expTime = reader.getUInt16(offset);
	            float expTimeFlt = (float)Math.pow(2, 16 - (expTime / 256));
	            DecimalFormat format = new DecimalFormat("0.#############");
	            format.setRoundingMode(RoundingMode.HALF_UP);
	            setFloat(TAG_SONY_EXPOSURE_TIME, expTimeFlt);

	            // SonyFNumber
	            offset = TAG_SONY_F_NUMBER;
	            int fNumber = reader.getUInt16(offset);
	            setInt(TAG_SONY_F_NUMBER, fNumber);

	            // ReleaseMode2
	            // ReleaseMode2

	            offset = TAG_INTERNAL_SERIAL_NUMBER;
	            int serialNum0 = reader.getUInt8(offset);
	            int serialNum1 = reader.getUInt8(offset + 1);
	            int serialNum2 = reader.getUInt8(offset + 2);
	            int serialNum3 = reader.getUInt8(offset + 3);
	            int serialNum4 = reader.getUInt8(offset + 4);
	            int serialNum5 = reader.getUInt8(offset + 5);
	            int[] serialNumber =
	                new int[]{serialNum0, serialNum1, serialNum2, serialNum3, serialNum4, serialNum5};
	            setIntArray(TAG_INTERNAL_SERIAL_NUMBER, serialNumber);

	            // LensMount
	            offset = TAG_LENS_MOUNT;
	            int lensMount = reader.getUInt8(offset);
	            setInt(TAG_LENS_MOUNT, lensMount);

	            // LensFormat
	            offset = TAG_LENS_FORMAT;
	            int lensFormat = reader.getUInt8(offset);
	            setInt(TAG_LENS_FORMAT, lensFormat);

	            // LensType2
	            offset = TAG_LENS_TYPE_2;
	            int lensType2 = reader.getUInt16(offset);
	            setInt(TAG_LENS_TYPE_2, lensType2);

	            // DistortionCorrParamsPresent
	            offset = TAG_DISTORTION_CORR_PARAMS_PRESENT;
	            int distortCorrParamsPresent = reader.getUInt8(offset);
	            setInt(TAG_DISTORTION_CORR_PARAMS_PRESENT, distortCorrParamsPresent);

	            // APS-CSizeCapture
	            offset = TAG_APS_C_SIZE_CAPTURE;
	            int apsCSizeCapture = reader.getUInt8(offset);
	            setInt(TAG_APS_C_SIZE_CAPTURE, apsCSizeCapture);

	            // LensSpecFeatures
	            offset = TAG_LENS_SPEC_FEATURES;
	            byte[] lensSpecFeatures = reader.getBytes(offset, 2);
	            setByteArray(TAG_APS_C_SIZE_CAPTURE, lensSpecFeatures);

	            // ShutterCount3
	            // APS-CSizeCapture
	            // LensSpecFeatures

	        } catch (IOException e) {
	            addError(e.getMessage());
	        }

	        return;
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link NikonType1MakernoteDirectory}.
	 * <p>
	 * Type-1 is for E-Series cameras prior to (not including) E990.  For example: E700, E800, E900,
	 * E900S, E910, E950.
	 * <p>
	 * Makernote starts from ASCII string "Nikon". Data format is the same as IFD, but it starts from
	 * offset 0x08. This is the same as Olympus except start string. Example of actual data
	 * structure is shown below.
	 * <pre><code>
	 * :0000: 4E 69 6B 6F 6E 00 01 00-05 00 02 00 02 00 06 00 Nikon...........
	 * :0010: 00 00 EC 02 00 00 03 00-03 00 01 00 00 00 06 00 ................
	 * </code></pre>
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class NikonType1MakernoteDescriptor extends TagDescriptor<NikonType1MakernoteDirectory>
	{
	    public NikonType1MakernoteDescriptor(@NotNull NikonType1MakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case NikonType1MakernoteDirectory.TAG_QUALITY:
	                return getQualityDescription();
	            case NikonType1MakernoteDirectory.TAG_COLOR_MODE:
	                return getColorModeDescription();
	            case NikonType1MakernoteDirectory.TAG_IMAGE_ADJUSTMENT:
	                return getImageAdjustmentDescription();
	            case NikonType1MakernoteDirectory.TAG_CCD_SENSITIVITY:
	                return getCcdSensitivityDescription();
	            case NikonType1MakernoteDirectory.TAG_WHITE_BALANCE:
	                return getWhiteBalanceDescription();
	            case NikonType1MakernoteDirectory.TAG_FOCUS:
	                return getFocusDescription();
	            case NikonType1MakernoteDirectory.TAG_DIGITAL_ZOOM:
	                return getDigitalZoomDescription();
	            case NikonType1MakernoteDirectory.TAG_CONVERTER:
	                return getConverterDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getConverterDescription()
	    {
	        return getIndexedDescription(NikonType1MakernoteDirectory.TAG_CONVERTER, "None", "Fisheye converter");
	    }

	    @Nullable
	    public String getDigitalZoomDescription()
	    {
	        Rational value = _directory.getRational(NikonType1MakernoteDirectory.TAG_DIGITAL_ZOOM);
	        return value == null
	            ? null
	            : value.getNumerator() == 0
	                ? "No digital zoom"
	                : value.toSimpleString(true) + "x digital zoom";
	    }

	    @Nullable
	    public String getFocusDescription()
	    {
	        Rational value = _directory.getRational(NikonType1MakernoteDirectory.TAG_FOCUS);
	        return value == null
	            ? null
	            : value.getNumerator() == 1 && value.getDenominator() == 0
	                ? "Infinite"
	                : value.toSimpleString(true);
	    }

	    @Nullable
	    public String getWhiteBalanceDescription()
	    {
	        return getIndexedDescription(NikonType1MakernoteDirectory.TAG_WHITE_BALANCE,
	            "Auto",
	            "Preset",
	            "Daylight",
	            "Incandescence",
	            "Florescence",
	            "Cloudy",
	            "SpeedLight"
	        );
	    }

	    @Nullable
	    public String getCcdSensitivityDescription()
	    {
	        return getIndexedDescription(NikonType1MakernoteDirectory.TAG_CCD_SENSITIVITY,
	            "ISO80",
	            null,
	            "ISO160",
	            null,
	            "ISO320",
	            "ISO100"
	        );
	    }

	    @Nullable
	    public String getImageAdjustmentDescription()
	    {
	        return getIndexedDescription(NikonType1MakernoteDirectory.TAG_IMAGE_ADJUSTMENT,
	            "Normal",
	            "Bright +",
	            "Bright -",
	            "Contrast +",
	            "Contrast -"
	        );
	    }

	    @Nullable
	    public String getColorModeDescription()
	    {
	        return getIndexedDescription(NikonType1MakernoteDirectory.TAG_COLOR_MODE,
	            1,
	            "Color",
	            "Monochrome"
	        );
	    }

	    @Nullable
	    public String getQualityDescription()
	    {
	        return getIndexedDescription(NikonType1MakernoteDirectory.TAG_QUALITY,
	            1,
	            "VGA Basic",
	            "VGA Normal",
	            "VGA Fine",
	            "SXGA Basic",
	            "SXGA Normal",
	            "SXGA Fine"
	        );
	    }
	}


	/**
	 * Describes tags specific to Nikon (type 1) cameras.  Type-1 is for E-Series cameras prior to (not including) E990.
	 *
	 * There are 3 formats of Nikon's Makernote. Makernote of E700/E800/E900/E900S/E910/E950
	 * starts from ASCII string "Nikon". Data format is the same as IFD, but it starts from
	 * offset 0x08. This is the same as Olympus except start string. Example of actual data
	 * structure is shown below.
	 * <pre><code>
	 * :0000: 4E 69 6B 6F 6E 00 01 00-05 00 02 00 02 00 06 00 Nikon...........
	 * :0010: 00 00 EC 02 00 00 03 00-03 00 01 00 00 00 06 00 ................
	 * </code></pre>
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class NikonType1MakernoteDirectory extends Directory
	{
	    public static final int TAG_UNKNOWN_1 = 0x0002;
	    public static final int TAG_QUALITY = 0x0003;
	    public static final int TAG_COLOR_MODE = 0x0004;
	    public static final int TAG_IMAGE_ADJUSTMENT = 0x0005;
	    public static final int TAG_CCD_SENSITIVITY = 0x0006;
	    public static final int TAG_WHITE_BALANCE = 0x0007;
	    public static final int TAG_FOCUS = 0x0008;
	    public static final int TAG_UNKNOWN_2 = 0x0009;
	    public static final int TAG_DIGITAL_ZOOM = 0x000A;
	    public static final int TAG_CONVERTER = 0x000B;
	    public static final int TAG_UNKNOWN_3 = 0x0F00;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    {
	        _tagNameMap.put(TAG_CCD_SENSITIVITY, "CCD Sensitivity");
	        _tagNameMap.put(TAG_COLOR_MODE, "Color Mode");
	        _tagNameMap.put(TAG_DIGITAL_ZOOM, "Digital Zoom");
	        _tagNameMap.put(TAG_CONVERTER, "Fisheye Converter");
	        _tagNameMap.put(TAG_FOCUS, "Focus");
	        _tagNameMap.put(TAG_IMAGE_ADJUSTMENT, "Image Adjustment");
	        _tagNameMap.put(TAG_QUALITY, "Quality");
	        _tagNameMap.put(TAG_UNKNOWN_1, "Makernote Unknown 1");
	        _tagNameMap.put(TAG_UNKNOWN_2, "Makernote Unknown 2");
	        _tagNameMap.put(TAG_UNKNOWN_3, "Makernote Unknown 3");
	        _tagNameMap.put(TAG_WHITE_BALANCE, "White Balance");
	    }

	    public NikonType1MakernoteDirectory()
	    {
	        this.setDescriptor(new NikonType1MakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Nikon Makernote";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link NikonType2MakernoteDirectory}.
	 *
	 * Type-2 applies to the E990 and D-series cameras such as the D1, D70 and D100.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class NikonType2MakernoteDescriptor extends TagDescriptor<NikonType2MakernoteDirectory>
	{
	    public NikonType2MakernoteDescriptor(@NotNull NikonType2MakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType)
	        {
	            case NikonType2MakernoteDirectory.TAG_PROGRAM_SHIFT:
	                return getProgramShiftDescription();
	            case NikonType2MakernoteDirectory.TAG_EXPOSURE_DIFFERENCE:
	                return getExposureDifferenceDescription();
	            case NikonType2MakernoteDirectory.TAG_LENS:
	                return getLensDescription();
	            case NikonType2MakernoteDirectory.TAG_CAMERA_HUE_ADJUSTMENT:
	                return getHueAdjustmentDescription();
	            case NikonType2MakernoteDirectory.TAG_CAMERA_COLOR_MODE:
	                return getColorModeDescription();
	            case NikonType2MakernoteDirectory.TAG_AUTO_FLASH_COMPENSATION:
	                return getAutoFlashCompensationDescription();
	            case NikonType2MakernoteDirectory.TAG_FLASH_EXPOSURE_COMPENSATION:
	                return getFlashExposureCompensationDescription();
	            case NikonType2MakernoteDirectory.TAG_FLASH_BRACKET_COMPENSATION:
	                return getFlashBracketCompensationDescription();
	            case NikonType2MakernoteDirectory.TAG_EXPOSURE_TUNING:
	                return getExposureTuningDescription();
	            case NikonType2MakernoteDirectory.TAG_LENS_STOPS:
	                return getLensStopsDescription();
	            case NikonType2MakernoteDirectory.TAG_COLOR_SPACE:
	                return getColorSpaceDescription();
	            case NikonType2MakernoteDirectory.TAG_ACTIVE_D_LIGHTING:
	                return getActiveDLightingDescription();
	            case NikonType2MakernoteDirectory.TAG_VIGNETTE_CONTROL:
	                return getVignetteControlDescription();
	            case NikonType2MakernoteDirectory.TAG_ISO_1:
	                return getIsoSettingDescription();
	            case NikonType2MakernoteDirectory.TAG_DIGITAL_ZOOM:
	                return getDigitalZoomDescription();
	            case NikonType2MakernoteDirectory.TAG_FLASH_USED:
	                return getFlashUsedDescription();
	            case NikonType2MakernoteDirectory.TAG_AF_FOCUS_POSITION:
	                return getAutoFocusPositionDescription();
	            case NikonType2MakernoteDirectory.TAG_FIRMWARE_VERSION:
	                return getFirmwareVersionDescription();
	            case NikonType2MakernoteDirectory.TAG_LENS_TYPE:
	                return getLensTypeDescription();
	            case NikonType2MakernoteDirectory.TAG_SHOOTING_MODE:
	                return getShootingModeDescription();
	            case NikonType2MakernoteDirectory.TAG_NEF_COMPRESSION:
	                return getNEFCompressionDescription();
	            case NikonType2MakernoteDirectory.TAG_HIGH_ISO_NOISE_REDUCTION:
	                return getHighISONoiseReductionDescription();
	            case NikonType2MakernoteDirectory.TAG_POWER_UP_TIME:
	                return getPowerUpTimeDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getPowerUpTimeDescription()
	    {
	        // this is generally a byte[] of length 8 directly representing a date and time.
	        // the format is : first 2 bytes together are the year, and then each byte after
	        //                 is month, day, hour, minute, second with the eighth byte unused
	        // e.g., 2011:04:25 01:54:58

	        byte[] values = _directory.getByteArray(NikonType2MakernoteDirectory.TAG_POWER_UP_TIME);
	        if (values == null) {
	            return null;
	        }
	        short year = ByteBuffer.wrap(new byte[]{values[0], values[1]}).getShort();
	        return String.format("%04d:%02d:%02d %02d:%02d:%02d", year, values[2], values[3],
	                                                        values[4], values[5], values[6]);
	    }

	    @Nullable
	    public String getHighISONoiseReductionDescription()
	    {
	        return getIndexedDescription(NikonType2MakernoteDirectory.TAG_HIGH_ISO_NOISE_REDUCTION,
	            "Off",
	            "Minimal",
	            "Low",
	            null,
	            "Normal",
	            null,
	            "High"
	        );
	    }

	    @Nullable
	    public String getFlashUsedDescription()
	    {
	        return getIndexedDescription(NikonType2MakernoteDirectory.TAG_FLASH_USED,
	            "Flash Not Used",
	            "Manual Flash",
	            null,
	            "Flash Not Ready",
	            null,
	            null,
	            null,
	            "External Flash",
	            "Fired, Commander Mode",
	            "Fired, TTL Mode"
	        );
	    }

	    @Nullable
	    public String getNEFCompressionDescription()
	    {
	        return getIndexedDescription(NikonType2MakernoteDirectory.TAG_NEF_COMPRESSION,
	            1,
	            "Lossy (Type 1)",
	            null,
	            "Uncompressed",
	            null,
	            null,
	            null,
	            "Lossless",
	            "Lossy (Type 2)"
	        );
	    }

	    @Nullable
	    public String getShootingModeDescription()
	    {
	        return getBitFlagDescription(NikonType2MakernoteDirectory.TAG_SHOOTING_MODE,
	            // LSB [low label, high label]
	            new String[]{"Single Frame", "Continuous"},
	            "Delay",
	            null,
	            "PC Control",
	            "Exposure Bracketing",
	            "Auto ISO",
	            "White-Balance Bracketing",
	            "IR Control"
	        );
	    }

	    @Nullable
	    public String getLensTypeDescription()
	    {
	        return getBitFlagDescription(NikonType2MakernoteDirectory.TAG_LENS_TYPE,
	            // LSB [low label, high label]
	            new String[]{"AF", "MF"},
	            "D",
	            "G",
	            "VR"
	        );
	    }

	    @Nullable
	    public String getColorSpaceDescription()
	    {
	        return getIndexedDescription(NikonType2MakernoteDirectory.TAG_COLOR_SPACE,
	            1,
	            "sRGB",
	            "Adobe RGB"
	        );
	    }

	    @Nullable
	    public String getActiveDLightingDescription()
	    {
	        Integer value = _directory.getInteger(NikonType2MakernoteDirectory.TAG_ACTIVE_D_LIGHTING);
	        if (value==null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 1: return "Light";
	            case 3: return "Normal";
	            case 5: return "High";
	            case 7: return "Extra High";
	            case 65535: return "Auto";
	            default: return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getVignetteControlDescription()
	    {
	        Integer value = _directory.getInteger(NikonType2MakernoteDirectory.TAG_VIGNETTE_CONTROL);
	        if (value==null)
	            return null;
	        switch (value) {
	            case 0: return "Off";
	            case 1: return "Low";
	            case 3: return "Normal";
	            case 5: return "High";
	            default: return "Unknown (" + value + ")";
	        }
	    }

	    @Nullable
	    public String getAutoFocusPositionDescription()
	    {
	        int[] values = _directory.getIntArray(NikonType2MakernoteDirectory.TAG_AF_FOCUS_POSITION);
	        if (values==null)
	            return null;
	        if (values.length != 4 || values[0] != 0 || values[2] != 0 || values[3] != 0) {
	            return "Unknown (" + _directory.getString(NikonType2MakernoteDirectory.TAG_AF_FOCUS_POSITION) + ")";
	        }
	        switch (values[1]) {
	            case 0:
	                return "Centre";
	            case 1:
	                return "Top";
	            case 2:
	                return "Bottom";
	            case 3:
	                return "Left";
	            case 4:
	                return "Right";
	            default:
	                return "Unknown (" + values[1] + ")";
	        }
	    }

	    @Nullable
	    public String getDigitalZoomDescription()
	    {
	        Rational value = _directory.getRational(NikonType2MakernoteDirectory.TAG_DIGITAL_ZOOM);
	        if (value==null)
	            return null;
	        return value.intValue() == 1
	                ? "No digital zoom"
	                : value.toSimpleString(true) + "x digital zoom";
	    }

	    @Nullable
	    public String getProgramShiftDescription()
	    {
	        return getEVDescription(NikonType2MakernoteDirectory.TAG_PROGRAM_SHIFT);
	    }

	    @Nullable
	    public String getExposureDifferenceDescription()
	    {
	        return getEVDescription(NikonType2MakernoteDirectory.TAG_EXPOSURE_DIFFERENCE);
	    }

	    @Nullable
	    public String getAutoFlashCompensationDescription()
	    {
	        return getEVDescription(NikonType2MakernoteDirectory.TAG_AUTO_FLASH_COMPENSATION);
	    }

	    @Nullable
	    public String getFlashExposureCompensationDescription()
	    {
	        return getEVDescription(NikonType2MakernoteDirectory.TAG_FLASH_EXPOSURE_COMPENSATION);
	    }

	    @Nullable
	    public String getFlashBracketCompensationDescription()
	    {
	        return getEVDescription(NikonType2MakernoteDirectory.TAG_FLASH_BRACKET_COMPENSATION);
	    }

	    @Nullable
	    public String getExposureTuningDescription()
	    {
	        return getEVDescription(NikonType2MakernoteDirectory.TAG_EXPOSURE_TUNING);
	    }

	    @Nullable
	    public String getLensStopsDescription()
	    {
	        return getEVDescription(NikonType2MakernoteDirectory.TAG_LENS_STOPS);
	    }

	    @Nullable
	    private String getEVDescription(int tagType)
	    {
	        int[] values = _directory.getIntArray(tagType);
	        if (values == null || values.length < 2)
	            return null;
	        if (values.length < 3 || values[2] == 0)
	            return null;
	        final DecimalFormat decimalFormat = new DecimalFormat("0.##");
	        double ev = values[0] * values[1] / (double)values[2];
	        return decimalFormat.format(ev) + " EV";
	    }

	    @Nullable
	    public String getIsoSettingDescription()
	    {
	        int[] values = _directory.getIntArray(NikonType2MakernoteDirectory.TAG_ISO_1);
	        if (values == null)
	            return null;
	        if (values[0] != 0 || values[1] == 0)
	            return "Unknown (" + _directory.getString(NikonType2MakernoteDirectory.TAG_ISO_1) + ")";
	        return "ISO " + values[1];
	    }

	    @Nullable
	    public String getLensDescription()
	    {
	        return getLensSpecificationDescription(NikonType2MakernoteDirectory.TAG_LENS);
	    }

	    @Nullable
	    public String getLensFocusDistance()
	    {
	        int[] values = _directory.getDecryptedIntArray(NikonType2MakernoteDirectory.TAG_LENS_DATA);

	        if (values == null || values.length < 11)
	            return null;

	        return String.format("%.2fm", getDistanceInMeters(values[10]));
	    }

	    @Nullable
	    public String getHueAdjustmentDescription()
	    {
	        return getFormattedString(NikonType2MakernoteDirectory.TAG_CAMERA_HUE_ADJUSTMENT, "%s degrees");
	    }

	    @Nullable
	    public String getColorModeDescription()
	    {
	        String value = _directory.getString(NikonType2MakernoteDirectory.TAG_CAMERA_COLOR_MODE);
	        return value == null ? null : value.startsWith("MODE1") ? "Mode I (sRGB)" : value;
	    }

	    @Nullable
	    public String getFirmwareVersionDescription()
	    {
	        return getVersionBytesDescription(NikonType2MakernoteDirectory.TAG_FIRMWARE_VERSION, 2);
	    }

	    private double getDistanceInMeters(int val)
	    {
	        if (val < 0)
	            val += 256;
	        return 0.01 * Math.pow(10, val / 40.0f);
	    }
	}

	
	/**
	 * Describes tags specific to Nikon (type 2) cameras.  Type-2 applies to the E990 and D-series cameras such as the E990, D1,
	 * D70 and D100.
	 * <p>
	 * Thanks to Fabrizio Giudici for publishing his reverse-engineering of the D100 makernote data.
	 * http://www.timelesswanderings.net/equipment/D100/NEF.html
	 * <p>
	 * Note that the camera implements image protection (locking images) via the file's 'readonly' attribute.  Similarly
	 * image hiding uses the 'hidden' attribute (observed on the D70).  Consequently, these values are not available here.
	 * <p>
	 * Additional sample images have been observed, and their tag values recorded in javadoc comments for each tag's field.
	 * New tags have subsequently been added since Fabrizio's observations.
	 * <p>
	 * In earlier models (such as the E990 and D1), this directory begins at the first byte of the makernote IFD.  In
	 * later models, the IFD was given the standard prefix to indicate the camera models (most other manufacturers also
	 * provide this prefix to aid in software decoding).
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class NikonType2MakernoteDirectory extends Directory
	{
	    /**
	     * Values observed
	     * - 0200 (D70)
	     * - 0200 (D1X)
	     */
	    public static final int TAG_FIRMWARE_VERSION = 0x0001;

	    /**
	     * Values observed
	     * - 0 250
	     * - 0 400
	     */
	    public static final int TAG_ISO_1 = 0x0002;

	    /**
	     * The camera's color mode, as an uppercase string.  Examples include:
	     * <ul>
	     * <li><code>B &amp; W</code></li>
	     * <li><code>COLOR</code></li>
	     * <li><code>COOL</code></li>
	     * <li><code>SEPIA</code></li>
	     * <li><code>VIVID</code></li>
	     * </ul>
	     */
	    public static final int TAG_COLOR_MODE = 0x0003;

	    /**
	     * The camera's quality setting, as an uppercase string.  Examples include:
	     * <ul>
	     * <li><code>BASIC</code></li>
	     * <li><code>FINE</code></li>
	     * <li><code>NORMAL</code></li>
	     * <li><code>RAW</code></li>
	     * <li><code>RAW2.7M</code></li>
	     * </ul>
	     */
	    public static final int TAG_QUALITY_AND_FILE_FORMAT = 0x0004;

	    /**
	     * The camera's white balance setting, as an uppercase string.  Examples include:
	     *
	     * <ul>
	     * <li><code>AUTO</code></li>
	     * <li><code>CLOUDY</code></li>
	     * <li><code>FLASH</code></li>
	     * <li><code>FLUORESCENT</code></li>
	     * <li><code>INCANDESCENT</code></li>
	     * <li><code>PRESET</code></li>
	     * <li><code>PRESET0</code></li>
	     * <li><code>PRESET1</code></li>
	     * <li><code>PRESET3</code></li>
	     * <li><code>SUNNY</code></li>
	     * <li><code>WHITE PRESET</code></li>
	     * <li><code>4350K</code></li>
	     * <li><code>5000K</code></li>
	     * <li><code>DAY WHITE FL</code></li>
	     * <li><code>SHADE</code></li>
	     * </ul>
	     */
	    public static final int TAG_CAMERA_WHITE_BALANCE  = 0x0005;

	    /**
	     * The camera's sharpening setting, as an uppercase string.  Examples include:
	     *
	     * <ul>
	     * <li><code>AUTO</code></li>
	     * <li><code>HIGH</code></li>
	     * <li><code>LOW</code></li>
	     * <li><code>NONE</code></li>
	     * <li><code>NORMAL</code></li>
	     * <li><code>MED.H</code></li>
	     * <li><code>MED.L</code></li>
	     * </ul>
	     */
	    public static final int TAG_CAMERA_SHARPENING = 0x0006;

	    /**
	     * The camera's auto-focus mode, as an uppercase string.  Examples include:
	     *
	     * <ul>
	     * <li><code>AF-C</code></li>
	     * <li><code>AF-S</code></li>
	     * <li><code>MANUAL</code></li>
	     * <li><code>AF-A</code></li>
	     * </ul>
	     */
	    public static final int TAG_AF_TYPE = 0x0007;

	    /**
	     * The camera's flash setting, as an uppercase string.  Examples include:
	     *
	     * <ul>
	     * <li><code></code></li>
	     * <li><code>NORMAL</code></li>
	     * <li><code>RED-EYE</code></li>
	     * <li><code>SLOW</code></li>
	     * <li><code>NEW_TTL</code></li>
	     * <li><code>REAR</code></li>
	     * <li><code>REAR SLOW</code></li>
	     * </ul>
	     * Note: when TAG_AUTO_FLASH_MODE is blank (whitespace), Nikon Browser displays "Flash Sync Mode: Not Attached"
	     */
	    public static final int TAG_FLASH_SYNC_MODE = 0x0008;

	    /**
	     * The type of flash used in the photograph, as a string.  Examples include:
	     *
	     * <ul>
	     * <li><code></code></li>
	     * <li><code>Built-in,TTL</code></li>
	     * <li><code>NEW_TTL</code> Nikon Browser interprets as "D-TTL"</li>
	     * <li><code>Built-in,M</code></li>
	     * <li><code>Optional,TTL</code> with speedlight SB800, flash sync mode as "NORMAL"</li>
	     * </ul>
	     */
	    public static final int TAG_AUTO_FLASH_MODE = 0x0009;

	    /**
	     * An unknown tag, as a rational.  Several values given here:
	     * http://gvsoft.homedns.org/exif/makernote-nikon-type2.html#0x000b
	     */
	    public static final int TAG_UNKNOWN_34 = 0x000A;

	    /**
	     * The camera's white balance bias setting, as an uint16 array having either one or two elements.
	     *
	     * <ul>
	     * <li><code>0</code></li>
	     * <li><code>1</code></li>
	     * <li><code>-3</code></li>
	     * <li><code>-2</code></li>
	     * <li><code>-1</code></li>
	     * <li><code>0,0</code></li>
	     * <li><code>1,0</code></li>
	     * <li><code>5,-5</code></li>
	     * </ul>
	     */
	    public static final int TAG_CAMERA_WHITE_BALANCE_FINE = 0x000B;

	    /**
	     * The first two numbers are coefficients to multiply red and blue channels according to white balance as set in the
	     * camera. The meaning of the third and the fourth numbers is unknown.
	     *
	     * Values observed
	     * - 2.25882352 1.76078431 0.0 0.0
	     * - 10242/1 34305/1 0/1 0/1
	     * - 234765625/100000000 1140625/1000000 1/1 1/1
	     */
	    public static final int TAG_CAMERA_WHITE_BALANCE_RB_COEFF = 0x000C;

	    /**
	     * The camera's program shift setting, as an array of four integers.
	     * The value, in EV, is calculated as <code>a*b/c</code>.
	     *
	     * <ul>
	     * <li><code>0,1,3,0</code> = 0 EV</li>
	     * <li><code>1,1,3,0</code> = 0.33 EV</li>
	     * <li><code>-3,1,3,0</code> = -1 EV</li>
	     * <li><code>1,1,2,0</code> = 0.5 EV</li>
	     * <li><code>2,1,6,0</code> = 0.33 EV</li>
	     * </ul>
	     */
	    public static final int TAG_PROGRAM_SHIFT = 0x000D;

	    /**
	     * The exposure difference, as an array of four integers.
	     * The value, in EV, is calculated as <code>a*b/c</code>.
	     *
	     * <ul>
	     * <li><code>-105,1,12,0</code> = -8.75 EV</li>
	     * <li><code>-72,1,12,0</code> = -6.00 EV</li>
	     * <li><code>-11,1,12,0</code> = -0.92 EV</li>
	     * </ul>
	     */
	    public static final int TAG_EXPOSURE_DIFFERENCE = 0x000E;

	    /**
	     * The camera's ISO mode, as an uppercase string.
	     *
	     * <ul>
	     * <li><code>AUTO</code></li>
	     * <li><code>MANUAL</code></li>
	     * </ul>
	     */
	    public static final int TAG_ISO_MODE = 0x000F;

	    /**
	     * Added during merge of Type2 &amp; Type3.  May apply to earlier models, such as E990 and D1.
	     */
	    public static final int TAG_DATA_DUMP = 0x0010;

	    /**
	     * Preview to another IFD (?)
	     * <p>
	     * Details here: http://gvsoft.homedns.org/exif/makernote-nikon-2-tag0x0011.html
	     * // TODO if this is another IFD, decode it
	     */
	    public static final int TAG_PREVIEW_IFD = 0x0011;

	    /**
	     * The flash compensation, as an array of four integers.
	     * The value, in EV, is calculated as <code>a*b/c</code>.
	     *
	     * <ul>
	     * <li><code>-18,1,6,0</code> = -3 EV</li>
	     * <li><code>4,1,6,0</code> = 0.67 EV</li>
	     * <li><code>6,1,6,0</code> = 1 EV</li>
	     * </ul>
	     */
	    public static final int TAG_AUTO_FLASH_COMPENSATION = 0x0012;

	    /**
	     * The requested ISO value, as an array of two integers.
	     *
	     * <ul>
	     * <li><code>0,0</code></li>
	     * <li><code>0,125</code></li>
	     * <li><code>1,2500</code></li>
	     * </ul>
	     */
	    public static final int TAG_ISO_REQUESTED = 0x0013;

	    /**
	     * Defines the photo corner coordinates, in 8 bytes.  Treated as four 16-bit integers, they
	     * decode as: top-left (x,y); bot-right (x,y)
	     * - 0 0 49163 53255
	     * - 0 0 3008 2000 (the image dimensions were 3008x2000) (D70)
	     * <ul>
	     * <li><code>0,0,4288,2848</code> The max resolution of the D300 camera</li>
	     * <li><code>0,0,3008,2000</code> The max resolution of the D70 camera</li>
	     * <li><code>0,0,4256,2832</code> The max resolution of the D3 camera</li>
	     * </ul>
	     */
	    public static final int TAG_IMAGE_BOUNDARY = 0x0016;

	    /**
	     * The flash exposure compensation, as an array of four integers.
	     * The value, in EV, is calculated as <code>a*b/c</code>.
	     *
	     * <ul>
	     * <li><code>0,0,0,0</code> = 0 EV</li>
	     * <li><code>0,1,6,0</code> = 0 EV</li>
	     * <li><code>4,1,6,0</code> = 0.67 EV</li>
	     * </ul>
	     */
	    public static final int TAG_FLASH_EXPOSURE_COMPENSATION = 0x0017;

	    /**
	     * The flash bracket compensation, as an array of four integers.
	     * The value, in EV, is calculated as <code>a*b/c</code>.
	     *
	     * <ul>
	     * <li><code>0,0,0,0</code> = 0 EV</li>
	     * <li><code>0,1,6,0</code> = 0 EV</li>
	     * <li><code>4,1,6,0</code> = 0.67 EV</li>
	     * </ul>
	     */
	    public static final int TAG_FLASH_BRACKET_COMPENSATION = 0x0018;

	    /**
	     * The AE bracket compensation, as a rational number.
	     *
	     * <ul>
	     * <li><code>0/0</code></li>
	     * <li><code>0/1</code></li>
	     * <li><code>0/6</code></li>
	     * <li><code>4/6</code></li>
	     * <li><code>6/6</code></li>
	     * </ul>
	     */
	    public static final int TAG_AE_BRACKET_COMPENSATION = 0x0019;

	    /**
	     * Flash mode, as a string.
	     *
	     * <ul>
	     * <li><code></code></li>
	     * <li><code>Red Eye Reduction</code></li>
	     * <li><code>D-Lighting</code></li>
	     * <li><code>Distortion control</code></li>
	     * </ul>
	     */
	    public static final int TAG_FLASH_MODE = 0x001a;

	    public static final int TAG_CROP_HIGH_SPEED = 0x001b;
	    public static final int TAG_EXPOSURE_TUNING = 0x001c;

	    /**
	     * The camera's serial number, as a string.
	     * Note that D200 is always blank, and D50 is always <code>"D50"</code>.
	     */
	    public static final int TAG_CAMERA_SERIAL_NUMBER = 0x001d;

	    /**
	     * The camera's color space setting.
	     *
	     * <ul>
	     * <li><code>1</code> sRGB</li>
	     * <li><code>2</code> Adobe RGB</li>
	     * </ul>
	     */
	    public static final int TAG_COLOR_SPACE = 0x001e;
	    public static final int TAG_VR_INFO = 0x001f;
	    public static final int TAG_IMAGE_AUTHENTICATION = 0x0020;
	    public static final int TAG_UNKNOWN_35 = 0x0021;

	    /**
	     * The active D-Lighting setting.
	     *
	     * <ul>
	     * <li><code>0</code> Off</li>
	     * <li><code>1</code> Low</li>
	     * <li><code>3</code> Normal</li>
	     * <li><code>5</code> High</li>
	     * <li><code>7</code> Extra High</li>
	     * <li><code>65535</code> Auto</li>
	     * </ul>
	     */
	    public static final int TAG_ACTIVE_D_LIGHTING = 0x0022;
	    public static final int TAG_PICTURE_CONTROL = 0x0023;
	    public static final int TAG_WORLD_TIME = 0x0024;
	    public static final int TAG_ISO_INFO = 0x0025;
	    public static final int TAG_UNKNOWN_36 = 0x0026;
	    public static final int TAG_UNKNOWN_37 = 0x0027;
	    public static final int TAG_UNKNOWN_38 = 0x0028;
	    public static final int TAG_UNKNOWN_39 = 0x0029;

	    /**
	     * The camera's vignette control setting.
	     *
	     * <ul>
	     * <li><code>0</code> Off</li>
	     * <li><code>1</code> Low</li>
	     * <li><code>3</code> Normal</li>
	     * <li><code>5</code> High</li>
	     * </ul>
	     */
	    public static final int TAG_VIGNETTE_CONTROL = 0x002a;
	    public static final int TAG_UNKNOWN_40 = 0x002b;
	    public static final int TAG_UNKNOWN_41 = 0x002c;
	    public static final int TAG_UNKNOWN_42 = 0x002d;
	    public static final int TAG_UNKNOWN_43 = 0x002e;
	    public static final int TAG_UNKNOWN_44 = 0x002f;
	    public static final int TAG_UNKNOWN_45 = 0x0030;
	    public static final int TAG_UNKNOWN_46 = 0x0031;

	    /**
	     * The camera's image adjustment setting, as a string.
	     *
	     * <ul>
	     * <li><code>AUTO</code></li>
	     * <li><code>CONTRAST(+)</code></li>
	     * <li><code>CONTRAST(-)</code></li>
	     * <li><code>NORMAL</code></li>
	     * <li><code>B &amp; W</code></li>
	     * <li><code>BRIGHTNESS(+)</code></li>
	     * <li><code>BRIGHTNESS(-)</code></li>
	     * <li><code>SEPIA</code></li>
	     * </ul>
	     */
	    public static final int TAG_IMAGE_ADJUSTMENT = 0x0080;

	    /**
	     * The camera's tone compensation setting, as a string.
	     *
	     * <ul>
	     * <li><code>NORMAL</code></li>
	     * <li><code>LOW</code></li>
	     * <li><code>MED.L</code></li>
	     * <li><code>MED.H</code></li>
	     * <li><code>HIGH</code></li>
	     * <li><code>AUTO</code></li>
	     * </ul>
	     */
	    public static final int TAG_CAMERA_TONE_COMPENSATION = 0x0081;

	    /**
	     * A description of any auxiliary lens, as a string.
	     *
	     * <ul>
	     * <li><code>OFF</code></li>
	     * <li><code>FISHEYE 1</code></li>
	     * <li><code>FISHEYE 2</code></li>
	     * <li><code>TELEPHOTO 2</code></li>
	     * <li><code>WIDE ADAPTER</code></li>
	     * </ul>
	     */
	    public static final int TAG_ADAPTER = 0x0082;

	    /**
	     * The type of lens used, as a byte.
	     *
	     * <ul>
	     * <li><code>0x00</code> AF</li>
	     * <li><code>0x01</code> MF</li>
	     * <li><code>0x02</code> D</li>
	     * <li><code>0x06</code> G, D</li>
	     * <li><code>0x08</code> VR</li>
	     * <li><code>0x0a</code> VR, D</li>
	     * <li><code>0x0e</code> VR, G, D</li>
	     * </ul>
	     */
	    public static final int TAG_LENS_TYPE = 0x0083;

	    /**
	     * A pair of focal/max-fstop values that describe the lens used.
	     *
	     * Values observed
	     * - 180.0,180.0,2.8,2.8 (D100)
	     * - 240/10 850/10 35/10 45/10
	     * - 18-70mm f/3.5-4.5 (D70)
	     * - 17-35mm f/2.8-2.8 (D1X)
	     * - 70-200mm f/2.8-2.8 (D70)
	     *
	     * Nikon Browser identifies the lens as "18-70mm F/3.5-4.5 G" which
	     * is identical to metadata extractor, except for the "G".  This must
	     * be coming from another tag...
	     */
	    public static final int TAG_LENS = 0x0084;

	    /**
	     * Added during merge of Type2 &amp; Type3.  May apply to earlier models, such as E990 and D1.
	     */
	    public static final int TAG_MANUAL_FOCUS_DISTANCE = 0x0085;

	    /**
	     * The amount of digital zoom used.
	     */
	    public static final int TAG_DIGITAL_ZOOM = 0x0086;

	    /**
	     * Whether the flash was used in this image.
	     *
	     * <ul>
	     * <li><code>0</code> Flash Not Used</li>
	     * <li><code>1</code> Manual Flash</li>
	     * <li><code>3</code> Flash Not Ready</li>
	     * <li><code>7</code> External Flash</li>
	     * <li><code>8</code> Fired, Commander Mode</li>
	     * <li><code>9</code> Fired, TTL Mode</li>
	     * </ul>
	     */
	    public static final int TAG_FLASH_USED = 0x0087;

	    /**
	     * The position of the autofocus target.
	     */
	    public static final int TAG_AF_FOCUS_POSITION = 0x0088;

	    /**
	     * The camera's shooting mode.
	     * <p>
	     * A bit-array with:
	     * <ul>
	     * <li><code>0</code> Single Frame</li>
	     * <li><code>1</code> Continuous</li>
	     * <li><code>2</code> Delay</li>
	     * <li><code>8</code> PC Control</li>
	     * <li><code>16</code> Exposure Bracketing</li>
	     * <li><code>32</code> Auto ISO</li>
	     * <li><code>64</code> White-Balance Bracketing</li>
	     * <li><code>128</code> IR Control</li>
	     * </ul>
	     */
	    public static final int TAG_SHOOTING_MODE = 0x0089;

	    public static final int TAG_UNKNOWN_20 = 0x008A;

	    /**
	     * Lens stops, as an array of four integers.
	     * The value, in EV, is calculated as <code>a*b/c</code>.
	     *
	     * <ul>
	     * <li><code>64,1,12,0</code> = 5.33 EV</li>
	     * <li><code>72,1,12,0</code> = 6 EV</li>
	     * </ul>
	     */
	    public static final int TAG_LENS_STOPS = 0x008B;

	    public static final int TAG_CONTRAST_CURVE = 0x008C;

	    /**
	     * The color space as set in the camera, as a string.
	     *
	     * <ul>
	     * <li><code>MODE1</code> = Mode 1 (sRGB)</li>
	     * <li><code>MODE1a</code> = Mode 1 (sRGB)</li>
	     * <li><code>MODE2</code> = Mode 2 (Adobe RGB)</li>
	     * <li><code>MODE3</code> = Mode 2 (sRGB): Higher Saturation</li>
	     * <li><code>MODE3a</code> = Mode 2 (sRGB): Higher Saturation</li>
	     * <li><code>B &amp; W</code> = B &amp; W</li>
	     * </ul>
	     */
	    public static final int TAG_CAMERA_COLOR_MODE = 0x008D;
	    public static final int TAG_UNKNOWN_47 = 0x008E;

	    /**
	     * The camera's scene mode, as a string.  Examples include:
	     * <ul>
	     * <li><code>BEACH/SNOW</code></li>
	     * <li><code>CLOSE UP</code></li>
	     * <li><code>NIGHT PORTRAIT</code></li>
	     * <li><code>PORTRAIT</code></li>
	     * <li><code>ANTI-SHAKE</code></li>
	     * <li><code>BACK LIGHT</code></li>
	     * <li><code>BEST FACE</code></li>
	     * <li><code>BEST</code></li>
	     * <li><code>COPY</code></li>
	     * <li><code>DAWN/DUSK</code></li>
	     * <li><code>FACE-PRIORITY</code></li>
	     * <li><code>FIREWORKS</code></li>
	     * <li><code>FOOD</code></li>
	     * <li><code>HIGH SENS.</code></li>
	     * <li><code>LAND SCAPE</code></li>
	     * <li><code>MUSEUM</code></li>
	     * <li><code>PANORAMA ASSIST</code></li>
	     * <li><code>PARTY/INDOOR</code></li>
	     * <li><code>SCENE AUTO</code></li>
	     * <li><code>SMILE</code></li>
	     * <li><code>SPORT</code></li>
	     * <li><code>SPORT CONT.</code></li>
	     * <li><code>SUNSET</code></li>
	     * </ul>
	     */
	    public static final int TAG_SCENE_MODE = 0x008F;

	    /**
	     * The lighting type, as a string.  Examples include:
	     * <ul>
	     * <li><code></code></li>
	     * <li><code>NATURAL</code></li>
	     * <li><code>SPEEDLIGHT</code></li>
	     * <li><code>COLORED</code></li>
	     * <li><code>MIXED</code></li>
	     * <li><code>NORMAL</code></li>
	     * </ul>
	     */
	    public static final int TAG_LIGHT_SOURCE = 0x0090;

	    /**
	     * Advertised as ASCII, but actually isn't.  A variable number of bytes (eg. 18 to 533).  Actual number of bytes
	     * appears fixed for a given camera model.
	     */
	    public static final int TAG_SHOT_INFO = 0x0091;

	    /**
	     * The hue adjustment as set in the camera.  Values observed are either 0 or 3.
	     */
	    public static final int TAG_CAMERA_HUE_ADJUSTMENT = 0x0092;
	    /**
	     * The NEF (RAW) compression.  Examples include:
	     * <ul>
	     * <li><code>1</code> Lossy (Type 1)</li>
	     * <li><code>2</code> Uncompressed</li>
	     * <li><code>3</code> Lossless</li>
	     * <li><code>4</code> Lossy (Type 2)</li>
	     * </ul>
	     */
	    public static final int TAG_NEF_COMPRESSION = 0x0093;

	    /**
	     * The saturation level, as a signed integer.  Examples include:
	     * <ul>
	     * <li><code>+3</code></li>
	     * <li><code>+2</code></li>
	     * <li><code>+1</code></li>
	     * <li><code>0</code> Normal</li>
	     * <li><code>-1</code></li>
	     * <li><code>-2</code></li>
	     * <li><code>-3</code> (B&amp;W)</li>
	     * </ul>
	     */
	    public static final int TAG_SATURATION = 0x0094;

	    /**
	     * The type of noise reduction, as a string.  Examples include:
	     * <ul>
	     * <li><code>OFF</code></li>
	     * <li><code>FPNR</code></li>
	     * </ul>
	     */
	    public static final int TAG_NOISE_REDUCTION = 0x0095;
	    public static final int TAG_LINEARIZATION_TABLE = 0x0096;
	    public static final int TAG_COLOR_BALANCE = 0x0097;
	    public static final int TAG_LENS_DATA = 0x0098;

	    /** The NEF (RAW) thumbnail size, as an integer array with two items representing [width,height]. */
	    public static final int TAG_NEF_THUMBNAIL_SIZE = 0x0099;

	    /** The sensor pixel size, as a pair of rational numbers. */
	    public static final int TAG_SENSOR_PIXEL_SIZE = 0x009A;
	    public static final int TAG_UNKNOWN_10 = 0x009B;
	    public static final int TAG_SCENE_ASSIST = 0x009C;
	    public static final int TAG_UNKNOWN_11 = 0x009D;
	    public static final int TAG_RETOUCH_HISTORY = 0x009E;
	    public static final int TAG_UNKNOWN_12 = 0x009F;

	    /**
	     * The camera serial number, as a string.
	     * <ul>
	     * <li><code>NO= 00002539</code></li>
	     * <li><code>NO= -1000d71</code></li>
	     * <li><code>PKG597230621263</code></li>
	     * <li><code>PKG5995671330625116</code></li>
	     * <li><code>PKG49981281631130677</code></li>
	     * <li><code>BU672230725063</code></li>
	     * <li><code>NO= 200332c7</code></li>
	     * <li><code>NO= 30045efe</code></li>
	     * </ul>
	     */
	    public static final int TAG_CAMERA_SERIAL_NUMBER_2 = 0x00A0;

	    public static final int TAG_IMAGE_DATA_SIZE = 0x00A2;

	    public static final int TAG_UNKNOWN_27 = 0x00A3;
	    public static final int TAG_UNKNOWN_28 = 0x00A4;
	    public static final int TAG_IMAGE_COUNT = 0x00A5;
	    public static final int TAG_DELETED_IMAGE_COUNT = 0x00A6;

	    /** The number of total shutter releases.  This value increments for each exposure (observed on D70). */
	    public static final int TAG_EXPOSURE_SEQUENCE_NUMBER = 0x00A7;

	    public static final int TAG_FLASH_INFO = 0x00A8;
	    /**
	     * The camera's image optimisation, as a string.
	     * <ul>
	     *     <li><code></code></li>
	     *     <li><code>NORMAL</code></li>
	     *     <li><code>CUSTOM</code></li>
	     *     <li><code>BLACK AND WHITE</code></li>
	     *     <li><code>LAND SCAPE</code></li>
	     *     <li><code>MORE VIVID</code></li>
	     *     <li><code>PORTRAIT</code></li>
	     *     <li><code>SOFT</code></li>
	     *     <li><code>VIVID</code></li>
	     * </ul>
	     */
	    public static final int TAG_IMAGE_OPTIMISATION = 0x00A9;

	    /**
	     * The camera's saturation level, as a string.
	     * <ul>
	     *     <li><code></code></li>
	     *     <li><code>NORMAL</code></li>
	     *     <li><code>AUTO</code></li>
	     *     <li><code>ENHANCED</code></li>
	     *     <li><code>MODERATE</code></li>
	     * </ul>
	     */
	    public static final int TAG_SATURATION_2 = 0x00AA;

	    /**
	     * The camera's digital vari-program setting, as a string.
	     * <ul>
	     *     <li><code></code></li>
	     *     <li><code>AUTO</code></li>
	     *     <li><code>AUTO(FLASH OFF)</code></li>
	     *     <li><code>CLOSE UP</code></li>
	     *     <li><code>LANDSCAPE</code></li>
	     *     <li><code>NIGHT PORTRAIT</code></li>
	     *     <li><code>PORTRAIT</code></li>
	     *     <li><code>SPORT</code></li>
	     * </ul>
	     */
	    public static final int TAG_DIGITAL_VARI_PROGRAM = 0x00AB;

	    /**
	     * The camera's digital vari-program setting, as a string.
	     * <ul>
	     *     <li><code></code></li>
	     *     <li><code>VR-ON</code></li>
	     *     <li><code>VR-OFF</code></li>
	     *     <li><code>VR-HYBRID</code></li>
	     *     <li><code>VR-ACTIVE</code></li>
	     * </ul>
	     */
	    public static final int TAG_IMAGE_STABILISATION = 0x00AC;

	    /**
	     * The camera's digital vari-program setting, as a string.
	     * <ul>
	     *     <li><code></code></li>
	     *     <li><code>HYBRID</code></li>
	     *     <li><code>STANDARD</code></li>
	     * </ul>
	     */
	    public static final int TAG_AF_RESPONSE = 0x00AD;
	    public static final int TAG_UNKNOWN_29 = 0x00AE;
	    public static final int TAG_UNKNOWN_30 = 0x00AF;
	    public static final int TAG_MULTI_EXPOSURE = 0x00B0;

	    /**
	     * The camera's high ISO noise reduction setting, as an integer.
	     * <ul>
	     *     <li><code>0</code> Off</li>
	     *     <li><code>1</code> Minimal</li>
	     *     <li><code>2</code> Low</li>
	     *     <li><code>4</code> Normal</li>
	     *     <li><code>6</code> High</li>
	     * </ul>
	     */
	    public static final int TAG_HIGH_ISO_NOISE_REDUCTION = 0x00B1;
	    public static final int TAG_UNKNOWN_31 = 0x00B2;
	    public static final int TAG_UNKNOWN_32 = 0x00B3;
	    public static final int TAG_UNKNOWN_33 = 0x00B4;
	    public static final int TAG_UNKNOWN_48 = 0x00B5;
	    public static final int TAG_POWER_UP_TIME = 0x00B6;
	    public static final int TAG_AF_INFO_2 = 0x00B7;
	    public static final int TAG_FILE_INFO = 0x00B8;
	    public static final int TAG_AF_TUNE = 0x00B9;
	    public static final int TAG_UNKNOWN_49 = 0x00BB;
	    public static final int TAG_UNKNOWN_50 = 0x00BD;
	    public static final int TAG_UNKNOWN_51 = 0x0103;
	    public static final int TAG_PRINT_IMAGE_MATCHING_INFO = 0x0E00;

	    /**
	     * Data about changes set by Nikon Capture Editor.
	     *
	     * Values observed
	     */
	    public static final int TAG_NIKON_CAPTURE_DATA = 0x0E01;
	    public static final int TAG_UNKNOWN_52 = 0x0E05;
	    public static final int TAG_UNKNOWN_53 = 0x0E08;
	    public static final int TAG_NIKON_CAPTURE_VERSION = 0x0E09;
	    public static final int TAG_NIKON_CAPTURE_OFFSETS = 0x0E0E;
	    public static final int TAG_NIKON_SCAN = 0x0E10;
	    public static final int TAG_UNKNOWN_54 = 0x0E19;
	    public static final int TAG_NEF_BIT_DEPTH = 0x0E22;
	    public static final int TAG_UNKNOWN_55 = 0x0E23;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    {
	        _tagNameMap.put(TAG_FIRMWARE_VERSION, "Firmware Version");
	        _tagNameMap.put(TAG_ISO_1, "ISO");
	        _tagNameMap.put(TAG_QUALITY_AND_FILE_FORMAT, "Quality & File Format");
	        _tagNameMap.put(TAG_CAMERA_WHITE_BALANCE, "White Balance");
	        _tagNameMap.put(TAG_CAMERA_SHARPENING, "Sharpening");
	        _tagNameMap.put(TAG_AF_TYPE, "AF Type");
	        _tagNameMap.put(TAG_CAMERA_WHITE_BALANCE_FINE, "White Balance Fine");
	        _tagNameMap.put(TAG_CAMERA_WHITE_BALANCE_RB_COEFF, "White Balance RB Coefficients");
	        _tagNameMap.put(TAG_ISO_REQUESTED, "ISO");
	        _tagNameMap.put(TAG_ISO_MODE, "ISO Mode");
	        _tagNameMap.put(TAG_DATA_DUMP, "Data Dump");

	        _tagNameMap.put(TAG_PROGRAM_SHIFT, "Program Shift");
	        _tagNameMap.put(TAG_EXPOSURE_DIFFERENCE, "Exposure Difference");
	        _tagNameMap.put(TAG_PREVIEW_IFD, "Preview IFD");
	        _tagNameMap.put(TAG_LENS_TYPE, "Lens Type");
	        _tagNameMap.put(TAG_FLASH_USED, "Flash Used");
	        _tagNameMap.put(TAG_AF_FOCUS_POSITION, "AF Focus Position");
	        _tagNameMap.put(TAG_SHOOTING_MODE, "Shooting Mode");
	        _tagNameMap.put(TAG_LENS_STOPS, "Lens Stops");
	        _tagNameMap.put(TAG_CONTRAST_CURVE, "Contrast Curve");
	        _tagNameMap.put(TAG_LIGHT_SOURCE, "Light source");
	        _tagNameMap.put(TAG_SHOT_INFO, "Shot Info");
	        _tagNameMap.put(TAG_COLOR_BALANCE, "Color Balance");
	        _tagNameMap.put(TAG_LENS_DATA, "Lens Data");
	        _tagNameMap.put(TAG_NEF_THUMBNAIL_SIZE, "NEF Thumbnail Size");
	        _tagNameMap.put(TAG_SENSOR_PIXEL_SIZE, "Sensor Pixel Size");
	        _tagNameMap.put(TAG_UNKNOWN_10, "Unknown 10");
	        _tagNameMap.put(TAG_SCENE_ASSIST, "Scene Assist");
	        _tagNameMap.put(TAG_UNKNOWN_11, "Unknown 11");
	        _tagNameMap.put(TAG_RETOUCH_HISTORY, "Retouch History");
	        _tagNameMap.put(TAG_UNKNOWN_12, "Unknown 12");
	        _tagNameMap.put(TAG_FLASH_SYNC_MODE, "Flash Sync Mode");
	        _tagNameMap.put(TAG_AUTO_FLASH_MODE, "Auto Flash Mode");
	        _tagNameMap.put(TAG_AUTO_FLASH_COMPENSATION, "Auto Flash Compensation");
	        _tagNameMap.put(TAG_EXPOSURE_SEQUENCE_NUMBER, "Exposure Sequence Number");
	        _tagNameMap.put(TAG_COLOR_MODE, "Color Mode");

	        _tagNameMap.put(TAG_UNKNOWN_20, "Unknown 20");
	        _tagNameMap.put(TAG_IMAGE_BOUNDARY, "Image Boundary");
	        _tagNameMap.put(TAG_FLASH_EXPOSURE_COMPENSATION, "Flash Exposure Compensation");
	        _tagNameMap.put(TAG_FLASH_BRACKET_COMPENSATION, "Flash Bracket Compensation");
	        _tagNameMap.put(TAG_AE_BRACKET_COMPENSATION, "AE Bracket Compensation");
	        _tagNameMap.put(TAG_FLASH_MODE, "Flash Mode");
	        _tagNameMap.put(TAG_CROP_HIGH_SPEED, "Crop High Speed");
	        _tagNameMap.put(TAG_EXPOSURE_TUNING, "Exposure Tuning");
	        _tagNameMap.put(TAG_CAMERA_SERIAL_NUMBER, "Camera Serial Number");
	        _tagNameMap.put(TAG_COLOR_SPACE, "Color Space");
	        _tagNameMap.put(TAG_VR_INFO, "VR Info");
	        _tagNameMap.put(TAG_IMAGE_AUTHENTICATION, "Image Authentication");
	        _tagNameMap.put(TAG_UNKNOWN_35, "Unknown 35");
	        _tagNameMap.put(TAG_ACTIVE_D_LIGHTING, "Active D-Lighting");
	        _tagNameMap.put(TAG_PICTURE_CONTROL, "Picture Control");
	        _tagNameMap.put(TAG_WORLD_TIME, "World Time");
	        _tagNameMap.put(TAG_ISO_INFO, "ISO Info");
	        _tagNameMap.put(TAG_UNKNOWN_36, "Unknown 36");
	        _tagNameMap.put(TAG_UNKNOWN_37, "Unknown 37");
	        _tagNameMap.put(TAG_UNKNOWN_38, "Unknown 38");
	        _tagNameMap.put(TAG_UNKNOWN_39, "Unknown 39");
	        _tagNameMap.put(TAG_VIGNETTE_CONTROL, "Vignette Control");
	        _tagNameMap.put(TAG_UNKNOWN_40, "Unknown 40");
	        _tagNameMap.put(TAG_UNKNOWN_41, "Unknown 41");
	        _tagNameMap.put(TAG_UNKNOWN_42, "Unknown 42");
	        _tagNameMap.put(TAG_UNKNOWN_43, "Unknown 43");
	        _tagNameMap.put(TAG_UNKNOWN_44, "Unknown 44");
	        _tagNameMap.put(TAG_UNKNOWN_45, "Unknown 45");
	        _tagNameMap.put(TAG_UNKNOWN_46, "Unknown 46");
	        _tagNameMap.put(TAG_UNKNOWN_47, "Unknown 47");
	        _tagNameMap.put(TAG_SCENE_MODE, "Scene Mode");

	        _tagNameMap.put(TAG_CAMERA_SERIAL_NUMBER_2, "Camera Serial Number");
	        _tagNameMap.put(TAG_IMAGE_DATA_SIZE, "Image Data Size");
	        _tagNameMap.put(TAG_UNKNOWN_27, "Unknown 27");
	        _tagNameMap.put(TAG_UNKNOWN_28, "Unknown 28");
	        _tagNameMap.put(TAG_IMAGE_COUNT, "Image Count");
	        _tagNameMap.put(TAG_DELETED_IMAGE_COUNT, "Deleted Image Count");
	        _tagNameMap.put(TAG_SATURATION_2, "Saturation");
	        _tagNameMap.put(TAG_DIGITAL_VARI_PROGRAM, "Digital Vari Program");
	        _tagNameMap.put(TAG_IMAGE_STABILISATION, "Image Stabilisation");
	        _tagNameMap.put(TAG_AF_RESPONSE, "AF Response");
	        _tagNameMap.put(TAG_UNKNOWN_29, "Unknown 29");
	        _tagNameMap.put(TAG_UNKNOWN_30, "Unknown 30");
	        _tagNameMap.put(TAG_MULTI_EXPOSURE, "Multi Exposure");
	        _tagNameMap.put(TAG_HIGH_ISO_NOISE_REDUCTION, "High ISO Noise Reduction");
	        _tagNameMap.put(TAG_UNKNOWN_31, "Unknown 31");
	        _tagNameMap.put(TAG_UNKNOWN_32, "Unknown 32");
	        _tagNameMap.put(TAG_UNKNOWN_33, "Unknown 33");
	        _tagNameMap.put(TAG_UNKNOWN_48, "Unknown 48");
	        _tagNameMap.put(TAG_POWER_UP_TIME, "Power Up Time");
	        _tagNameMap.put(TAG_AF_INFO_2, "AF Info 2");
	        _tagNameMap.put(TAG_FILE_INFO, "File Info");
	        _tagNameMap.put(TAG_AF_TUNE, "AF Tune");
	        _tagNameMap.put(TAG_FLASH_INFO, "Flash Info");
	        _tagNameMap.put(TAG_IMAGE_OPTIMISATION, "Image Optimisation");

	        _tagNameMap.put(TAG_IMAGE_ADJUSTMENT, "Image Adjustment");
	        _tagNameMap.put(TAG_CAMERA_TONE_COMPENSATION, "Tone Compensation");
	        _tagNameMap.put(TAG_ADAPTER, "Adapter");
	        _tagNameMap.put(TAG_LENS, "Lens");
	        _tagNameMap.put(TAG_MANUAL_FOCUS_DISTANCE, "Manual Focus Distance");
	        _tagNameMap.put(TAG_DIGITAL_ZOOM, "Digital Zoom");
	        _tagNameMap.put(TAG_CAMERA_COLOR_MODE, "Colour Mode");
	        _tagNameMap.put(TAG_CAMERA_HUE_ADJUSTMENT, "Camera Hue Adjustment");
	        _tagNameMap.put(TAG_NEF_COMPRESSION, "NEF Compression");
	        _tagNameMap.put(TAG_SATURATION, "Saturation");
	        _tagNameMap.put(TAG_NOISE_REDUCTION, "Noise Reduction");
	        _tagNameMap.put(TAG_LINEARIZATION_TABLE, "Linearization Table");
	        _tagNameMap.put(TAG_NIKON_CAPTURE_DATA, "Nikon Capture Data");
	        _tagNameMap.put(TAG_UNKNOWN_49, "Unknown 49");
	        _tagNameMap.put(TAG_UNKNOWN_50, "Unknown 50");
	        _tagNameMap.put(TAG_UNKNOWN_51, "Unknown 51");
	        _tagNameMap.put(TAG_PRINT_IMAGE_MATCHING_INFO, "Print IM");
	        _tagNameMap.put(TAG_UNKNOWN_52, "Unknown 52");
	        _tagNameMap.put(TAG_UNKNOWN_53, "Unknown 53");
	        _tagNameMap.put(TAG_NIKON_CAPTURE_VERSION, "Nikon Capture Version");
	        _tagNameMap.put(TAG_NIKON_CAPTURE_OFFSETS, "Nikon Capture Offsets");
	        _tagNameMap.put(TAG_NIKON_SCAN, "Nikon Scan");
	        _tagNameMap.put(TAG_UNKNOWN_54, "Unknown 54");
	        _tagNameMap.put(TAG_NEF_BIT_DEPTH, "NEF Bit Depth");
	        _tagNameMap.put(TAG_UNKNOWN_55, "Unknown 55");
	    }

	    public NikonType2MakernoteDirectory()
	    {
	        this.setDescriptor(new NikonType2MakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Nikon Makernote";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }

		/** Nikon decryption tables used in exiftool */
	    private final int[] _decTable1 =   {0xc1,0xbf,0x6d,0x0d,0x59,0xc5,0x13,0x9d,0x83,0x61,0x6b,0x4f,0xc7,0x7f,0x3d,0x3d,
	                                               0x53,0x59,0xe3,0xc7,0xe9,0x2f,0x95,0xa7,0x95,0x1f,0xdf,0x7f,0x2b,0x29,0xc7,0x0d,
	                                               0xdf,0x07,0xef,0x71,0x89,0x3d,0x13,0x3d,0x3b,0x13,0xfb,0x0d,0x89,0xc1,0x65,0x1f,
	                                               0xb3,0x0d,0x6b,0x29,0xe3,0xfb,0xef,0xa3,0x6b,0x47,0x7f,0x95,0x35,0xa7,0x47,0x4f,
	                                               0xc7,0xf1,0x59,0x95,0x35,0x11,0x29,0x61,0xf1,0x3d,0xb3,0x2b,0x0d,0x43,0x89,0xc1,
	                                               0x9d,0x9d,0x89,0x65,0xf1,0xe9,0xdf,0xbf,0x3d,0x7f,0x53,0x97,0xe5,0xe9,0x95,0x17,
	                                               0x1d,0x3d,0x8b,0xfb,0xc7,0xe3,0x67,0xa7,0x07,0xf1,0x71,0xa7,0x53,0xb5,0x29,0x89,
	                                               0xe5,0x2b,0xa7,0x17,0x29,0xe9,0x4f,0xc5,0x65,0x6d,0x6b,0xef,0x0d,0x89,0x49,0x2f,
	                                               0xb3,0x43,0x53,0x65,0x1d,0x49,0xa3,0x13,0x89,0x59,0xef,0x6b,0xef,0x65,0x1d,0x0b,
	                                               0x59,0x13,0xe3,0x4f,0x9d,0xb3,0x29,0x43,0x2b,0x07,0x1d,0x95,0x59,0x59,0x47,0xfb,
	                                               0xe5,0xe9,0x61,0x47,0x2f,0x35,0x7f,0x17,0x7f,0xef,0x7f,0x95,0x95,0x71,0xd3,0xa3,
	                                               0x0b,0x71,0xa3,0xad,0x0b,0x3b,0xb5,0xfb,0xa3,0xbf,0x4f,0x83,0x1d,0xad,0xe9,0x2f,
	                                               0x71,0x65,0xa3,0xe5,0x07,0x35,0x3d,0x0d,0xb5,0xe9,0xe5,0x47,0x3b,0x9d,0xef,0x35,
	                                               0xa3,0xbf,0xb3,0xdf,0x53,0xd3,0x97,0x53,0x49,0x71,0x07,0x35,0x61,0x71,0x2f,0x43,
	                                               0x2f,0x11,0xdf,0x17,0x97,0xfb,0x95,0x3b,0x7f,0x6b,0xd3,0x25,0xbf,0xad,0xc7,0xc5,
	                                               0xc5,0xb5,0x8b,0xef,0x2f,0xd3,0x07,0x6b,0x25,0x49,0x95,0x25,0x49,0x6d,0x71,0xc7 };
	    private final int[] _decTable2 = { 0xa7,0xbc,0xc9,0xad,0x91,0xdf,0x85,0xe5,0xd4,0x78,0xd5,0x17,0x46,0x7c,0x29,0x4c,
	                                               0x4d,0x03,0xe9,0x25,0x68,0x11,0x86,0xb3,0xbd,0xf7,0x6f,0x61,0x22,0xa2,0x26,0x34,
	                                               0x2a,0xbe,0x1e,0x46,0x14,0x68,0x9d,0x44,0x18,0xc2,0x40,0xf4,0x7e,0x5f,0x1b,0xad,
	                                               0x0b,0x94,0xb6,0x67,0xb4,0x0b,0xe1,0xea,0x95,0x9c,0x66,0xdc,0xe7,0x5d,0x6c,0x05,
	                                               0xda,0xd5,0xdf,0x7a,0xef,0xf6,0xdb,0x1f,0x82,0x4c,0xc0,0x68,0x47,0xa1,0xbd,0xee,
	                                               0x39,0x50,0x56,0x4a,0xdd,0xdf,0xa5,0xf8,0xc6,0xda,0xca,0x90,0xca,0x01,0x42,0x9d,
	                                               0x8b,0x0c,0x73,0x43,0x75,0x05,0x94,0xde,0x24,0xb3,0x80,0x34,0xe5,0x2c,0xdc,0x9b,
	                                               0x3f,0xca,0x33,0x45,0xd0,0xdb,0x5f,0xf5,0x52,0xc3,0x21,0xda,0xe2,0x22,0x72,0x6b,
	                                               0x3e,0xd0,0x5b,0xa8,0x87,0x8c,0x06,0x5d,0x0f,0xdd,0x09,0x19,0x93,0xd0,0xb9,0xfc,
	                                               0x8b,0x0f,0x84,0x60,0x33,0x1c,0x9b,0x45,0xf1,0xf0,0xa3,0x94,0x3a,0x12,0x77,0x33,
	                                               0x4d,0x44,0x78,0x28,0x3c,0x9e,0xfd,0x65,0x57,0x16,0x94,0x6b,0xfb,0x59,0xd0,0xc8,
	                                               0x22,0x36,0xdb,0xd2,0x63,0x98,0x43,0xa1,0x04,0x87,0x86,0xf7,0xa6,0x26,0xbb,0xd6,
	                                               0x59,0x4d,0xbf,0x6a,0x2e,0xaa,0x2b,0xef,0xe6,0x78,0xb6,0x4e,0xe0,0x2f,0xdc,0x7c,
	                                               0xbe,0x57,0x19,0x32,0x7e,0x2a,0xd0,0xb8,0xba,0x29,0x00,0x3c,0x52,0x7d,0xa8,0x49,
	                                               0x3b,0x2d,0xeb,0x25,0x49,0xfa,0xa3,0xaa,0x39,0xa7,0xc5,0xa7,0x50,0x11,0x36,0xfb,
	                                               0xc6,0x67,0x4a,0xf5,0xa5,0x12,0x65,0x7e,0xb0,0xdf,0xaf,0x4e,0xb3,0x61,0x7f,0x2f };


	    /** decryption algorithm adapted from exiftool */
	    @Nullable
	    public int[] getDecryptedIntArray(int tagType)
	    {
	        int[] data = getIntArray(tagType);
	        Integer serial = getInteger(TAG_CAMERA_SERIAL_NUMBER);
	        Integer count = getInteger(TAG_EXPOSURE_SEQUENCE_NUMBER);

	        if (data == null || serial == null || count == null)
	            return null;

	        int key = 0;
	        for (int i = 0; i < 4; i++)
	            key ^= (count >> (i * 8)) & 0xff;

	        int ci = _decTable1[serial & 0xff];
	        int cj = _decTable2[key];
	        int ck = 0x60;

	        for (int i = 4; i < data.length; i++)
	        {
	            cj = (cj + ci * ck) & 0xff;
	            ck = (ck + 1) & 0xff;
	            data[i] ^= cj;
	        }

	        return data;
	    }
	}
	
	/**
	 * Provides human-readable string representations of tag values stored in a {@link SonyType6MakernoteDirectory}.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class SonyType6MakernoteDescriptor extends TagDescriptor<SonyType6MakernoteDirectory>
	{
	    public SonyType6MakernoteDescriptor(@NotNull SonyType6MakernoteDirectory directory)
	    {
	        super(directory);
	    }

	    @Override
	    @Nullable
	    public String getDescription(int tagType)
	    {
	        switch (tagType) {
	            case SonyType6MakernoteDirectory.TAG_MAKERNOTE_THUMB_VERSION:
	                return getMakernoteThumbVersionDescription();
	            default:
	                return super.getDescription(tagType);
	        }
	    }

	    @Nullable
	    public String getMakernoteThumbVersionDescription()
	    {
	        return getVersionBytesDescription(SonyType6MakernoteDirectory.TAG_MAKERNOTE_THUMB_VERSION, 2);
	    }
	}


	/**
	 * Describes tags specific to Sony cameras that use the Sony Type 6 makernote tags.
	 *
	 * @author Drew Noakes https://drewnoakes.com
	 */
	//@SuppressWarnings("WeakerAccess")
	public class SonyType6MakernoteDirectory extends Directory
	{
	    public static final int TAG_MAKERNOTE_THUMB_OFFSET = 0x0513;
	    public static final int TAG_MAKERNOTE_THUMB_LENGTH = 0x0514;
//	    public static final int TAG_UNKNOWN_1 = 0x0515;
	    public static final int TAG_MAKERNOTE_THUMB_VERSION = 0x2000;

	    @NotNull
	    private final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

	    {
	        _tagNameMap.put(TAG_MAKERNOTE_THUMB_OFFSET, "Makernote Thumb Offset");
	        _tagNameMap.put(TAG_MAKERNOTE_THUMB_LENGTH, "Makernote Thumb Length");
//	        _tagNameMap.put(TAG_UNKNOWN_1, "Sony-6-0x0203");
	        _tagNameMap.put(TAG_MAKERNOTE_THUMB_VERSION, "Makernote Thumb Version");
	    }

	    public SonyType6MakernoteDirectory()
	    {
	        this.setDescriptor(new SonyType6MakernoteDescriptor(this));
	    }

	    @Override
	    @NotNull
	    public String getName()
	    {
	        return "Sony Makernote";
	    }

	    @Override
	    @NotNull
	    protected HashMap<Integer, String> getTagNameMap()
	    {
	        return _tagNameMap;
	    }
	}


}