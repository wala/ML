package com.ibm.wala.cast.python.parquet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.avro.Schema;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroParquetReader.Builder;
import org.apache.parquet.conf.ParquetConfiguration;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.io.LocalInputFile;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.ibm.wala.cast.python.jep.ast.CPythonAstToCAstTranslator;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.collections.HashSetFactory;

public class ParquetPythonReader {
	public static Logger LOGGER = Logger.getAnonymousLogger();

	private final Path path;
	private final ParquetConfiguration conf;

	public ParquetPythonReader(File file) throws IOException {
		this.path = file.toPath();
		this.conf = new PlainParquetConfiguration();
		this.conf.set("parquet.avro.readInt96AsFixed", "true");
		getRecords(1);
	}

	public String getSchema() throws IOException {
		ParquetReader<Object> parquetReader = makeReader();
		GenericData.Record firstRecord = (GenericData.Record) parquetReader.read();
		if (firstRecord == null) {
			throw new IOException("Can't process empty Parquet file");
		}
		return firstRecord.getSchema().toString(true);
	}

	private ParquetReader<Object> makeReader() throws IOException {
		ParquetReader<Object> parquetReader = makeBuilder().build();
		return parquetReader;
	}

	private Builder<Object> makeBuilder() {
		return AvroParquetReader.builder(new LocalInputFile(this.path), conf);
	}

	public int getNumRecords() throws IOException {
		try (ParquetReader<Object> parquetReader = makeBuilder().withDataModel(GenericData.get()).withConf(this.conf)
				.build()) {
			GenericData.Record value;
			int i = 0;
			while (true) {
				value = (GenericData.Record) parquetReader.read();
				if (value == null) {
					return i;
				} else {
					i++;
				}
			}
		}
	}

	public List<String> getRecords(int numRecords) throws IOException, IllegalArgumentException {
		try (ParquetReader<Object> parquetReader = makeBuilder().withDataModel(GenericData.get()).withConf(this.conf)
				.build()) {
			List<String> records = new ArrayList<>();
			GenericData.Record value;
			for (int i = 0; i < numRecords; i++) {
				value = (GenericData.Record) parquetReader.read();
				if (value == null) {
					LOGGER.info(String.format("Retrieved %d records", records.size()));
					return records;
				} else {
					String jsonRecord = deserialize(value.getSchema(), toByteArray(value.getSchema(), value))
							.toString();
					records.add(jsonRecord);
				}
			}
			LOGGER.info(String.format("Retrieved %d records", records.size()));
			return records;
		}
	}

	/**
	 * Correctly converts timestamp-milis LogicalType values to strings. Taken from
	 * https://stackoverflow.com/a/52041154/729819.
	 */
	private GenericRecord deserialize(Schema schema, byte[] data) throws IOException {
		InputStream is = new ByteArrayInputStream(data);
		Decoder decoder = DecoderFactory.get().binaryDecoder(is, null);
		DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema, schema, GenericData.get());
		return reader.read(null, decoder);
	}

	private byte[] toByteArray(Schema schema, GenericRecord genericRecord) throws IOException {
		GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
		writer.getData().addLogicalTypeConversion(new TimeConversions.TimestampMillisConversion());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);
		writer.write(genericRecord, encoder);
		encoder.flush();
		return baos.toByteArray();
	}

	public static Module read(String file) throws IOException {
		ParquetPythonReader r = new ParquetPythonReader(new File(file));
		int numRecords = r.getNumRecords();
		LOGGER.info(r + " read " + numRecords + " from " + file);
		return new Module() {
			public String toString() {
				StringBuffer sb = new StringBuffer();
				getEntries().forEachRemaining(me -> {
					sb.append(me.getName()).append("\n");
				});
				return sb.toString();
			}

			@Override
			public Iterator<? extends ModuleEntry> getEntries() {
				try {
					return r.getRecords(numRecords).stream().map(rec -> {
						JSONTokener toks = new JSONTokener(rec);
						JSONObject program = (JSONObject) toks.nextValue();
						return new SourceModule() {
							private final Module x = this;

							@Override
							public Iterator<? extends ModuleEntry> getEntries() {
								return Collections.singleton(this).iterator();
							}

							@Override
							public String getName() {
								return program.getString("title");
							}

							@Override
							public boolean isClassFile() {
								return false;
							}

							@Override
							public boolean isSourceFile() {
								return true;
							}

							@Override
							public InputStream getInputStream() {
								return new ByteArrayInputStream(program.getString("contents").getBytes());
							}

							@Override
							public boolean isModuleFile() {
								return false;
							}

							@Override
							public Module asModule() {
								return this;
							}

							@Override
							public String getClassName() {
								return null;
							}

							@Override
							public Module getContainer() {
								return x;
							}

							@Override
							public Reader getInputReader() {
								return new StringReader(program.getString("contents"));
							}

							@Override
							public URL getURL() {
								try {
									return new URL(program.getString("document"));
								} catch (MalformedURLException | JSONException e) {
									assert false;
									return null;
								}
							}

						};
					}).iterator();
				} catch (IllegalArgumentException | IOException e) {
					assert false;
					return null;
				}
			}
		};
	}

	public static void main(String... args) throws IOException, ClassHierarchyException {
		Module m = read(args[0]);
		LOGGER.info(m.toString());
		Set<SourceModule> code = HashSetFactory.make();
		m.getEntries().forEachRemaining(f -> {
			code.add((SourceModule)f);
		});
		IClassHierarchy cha = CPythonAstToCAstTranslator.load(code);
		System.err.println(cha);
	}

}
