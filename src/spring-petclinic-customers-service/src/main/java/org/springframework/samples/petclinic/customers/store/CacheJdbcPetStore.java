package org.springframework.samples.petclinic.customers.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import javax.cache.Cache;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.apache.ignite.cache.store.CacheStoreSession;
import org.springframework.samples.petclinic.customers.model.Pet;
import org.apache.ignite.lang.IgniteBiInClosure;
import org.apache.ignite.resources.CacheStoreSessionResource;

public class CacheJdbcPetStore extends CacheStoreAdapter<Long, Pet> {

	@CacheStoreSessionResource
	private CacheStoreSession ses;

	@Override
	public Pet load(Long key) {
		System.out.println(">>> Store load [key=" + key + ']');
		try (Connection conn = connection()) {
			try (PreparedStatement st = conn
					.prepareStatement("select * from pets where id = ?")) {
				st.setString(1, key.toString());

				ResultSet rs = st.executeQuery();

				return rs.next() ? new Pet(rs.getInt(1), rs.getString(2),
						rs.getString(3), rs.getInt(4), rs.getInt(4)) : null;
			} catch (SQLException e) {
				throw new CacheLoaderException("Failed to load object [key="
						+ key + ']', e);
			}
		} catch (SQLException e) {
			throw new CacheLoaderException("Failed to load: " + key, e);
		}
	}

	@Override
	public void write(Cache.Entry<? extends Long, ? extends Pet> entry) {
		Long key = entry.getKey();
		Pet val = entry.getValue();

		System.out
				.println(">>> Store write [key=" + key + ", val=" + val + ']');
		
		try {
			Connection conn = connection();
			int updated;
			try (PreparedStatement st = conn
					.prepareStatement("update pets set name = ?,birth_date=?,type_id=? where id = ?")) {
				st.setString(1, val.name);
				st.setString(2, val.birthDate);
				st.setInt(3, val.typeId);
				st.setLong(4, val.id);

				updated = st.executeUpdate();
			}
			if (updated == 0) {
				try (PreparedStatement st = conn
						.prepareStatement("insert into pets (id, name, birth_date, type_id, owner_id) values (?, ?, ?, ?,?)")) {
					st.setLong(1, val.id);
					st.setString(2, val.name);
					st.setString(3, val.birthDate);
					st.setInt(4, val.typeId);
					st.setInt(5, val.ownerId);
					st.executeUpdate();
				}
			}
		} catch (SQLException e) {
			throw new CacheWriterException("Failed to write object [key=" + key
					+ ", val=" + val + ']', e);
		}
	}

	@Override
	public void delete(Object key) {
		System.out.println(">>> Store delete [key=" + key + ']');
		try (Connection conn = connection()) {
			try (PreparedStatement st = conn
					.prepareStatement("delete from pets where id=?")) {
				st.setLong(1, (Long) key);

				st.executeUpdate();
			} catch (SQLException e) {
				throw new CacheWriterException("Failed to delete object [key="
						+ key + ']', e);
			}
		} catch (SQLException e) {
			throw new CacheLoaderException("Failed to load: " + key, e);
		}
	}

	@Override
	public void loadCache(IgniteBiInClosure<Long, Pet> clo, Object... objects) {

		try (Connection conn = connection()) {
			try (PreparedStatement stmt = conn
					.prepareStatement("select * from pets")) {
				ResultSet rs = stmt.executeQuery();
				int cnt = 0;
				while (rs.next()) {
					Pet pet = new Pet(rs.getInt(1), rs.getString(2),
							rs.getString(3), rs.getInt(4), rs.getInt(4));
					clo.apply(pet.id, pet);
					cnt++;
				}
				System.out.println(">>> Loaded " + cnt + " values into cache.");
			} catch (SQLException e) {
				throw new CacheLoaderException(
						"Failed to load values from cache store.", e);
			}
		} catch (SQLException e) {
			throw new CacheLoaderException("Failed to load: ", e);
		}
	}

	private Connection connection() throws SQLException {
		Connection conn = DriverManager.getConnection(
				"jdbc:mysql://mysql:3306/petclinic", "root", "root");

		conn.setAutoCommit(true);

		return conn;
	}
}