#ifndef DECRYPTER_HPP
#define DECRYPTER_HPP



class decrypter{

public:
	virtual unsigned char* decrypt(unsigned char* data, const char* id, int size);
};

// the types of the class factories
typedef decrypter* create_t();
typedef void destroy_t(decrypter*);

#endif
