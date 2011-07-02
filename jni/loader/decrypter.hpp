#ifndef DECRYPTER_HPP
#define DECRYPTER_HPP



class decrypter{

public:
	virtual char* decrypt(char* data, const char* id);
};

// the types of the class factories
typedef decrypter* create_t();
typedef void destroy_t(decrypter*);

#endif
